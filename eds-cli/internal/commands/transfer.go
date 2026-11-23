package commands

import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

var mediaTypePattern = service.MediaTypePattern

func mediaTypeOf(name string) string { return service.MediaTypeOf(name) }

// progress shows how far a transfer is, on standard error, when a person is
// watching; it never writes to standard output, which may carry the data.
type progress struct {
	c       *command
	label   string
	total   int64 // -1 if unknown
	done    int64
	shown   time.Time
	visible bool
	mu      sync.Mutex
}

func (c *command) progress(label string, total int64) *progress {
	return &progress{c: c, label: label, total: total, visible: c.env.ErrTTY && !c.json()}
}

func (p *progress) add(n int) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.done += int64(n)
	if !p.visible || time.Since(p.shown) < 200*time.Millisecond {
		return
	}
	p.shown = time.Now()
	p.draw()
}

func (p *progress) draw() {
	locale := p.c.locale()
	if p.total > 0 {
		fmt.Fprintf(p.c.env.Err, "\r\x1b[K%s  %3d %%  %s / %s", p.label, p.done*100/p.total, locale.Size(p.done), locale.Size(p.total))
	} else {
		fmt.Fprintf(p.c.env.Err, "\r\x1b[K%s  %s", p.label, locale.Size(p.done))
	}
}

// clear removes the progress line, before the result is written.
func (p *progress) clear() {
	p.mu.Lock()
	defer p.mu.Unlock()
	if p.visible && !p.shown.IsZero() {
		fmt.Fprint(p.c.env.Err, "\r\x1b[K")
	}
}

type progressReader struct {
	r io.Reader
	p *progress
}

func (r progressReader) Read(b []byte) (int, error) {
	n, err := r.r.Read(b)
	if n > 0 {
		r.p.add(n)
	}
	return n, err
}

// upload is a prepared upload: the file is registered in its room, or the
// file to replace is found, and only the content is left to send.
type upload struct {
	t       target
	dataID  string
	name    string
	created bool // registered by this command; removed again if the content fails
}

// prepare registers the file an upload will fill, or finds the one to
// replace.
func (c *command) prepare(ctx context.Context, t target, name, mediaType string, replace, parents bool) (*upload, error) {
	if err := service.CheckWrite(t.room); err != nil {
		return nil, err
	}
	dest, err := resolveDestination(ctx, t, name, parents)
	if err != nil {
		return nil, err
	}
	u := &upload{t: t, name: dest.name}
	if dest.existing != nil {
		if dest.existing.IsFolder() || !replace {
			e := exit.New(exit.Usage, "%s", i18n.Rune("A file or folder with this name already exists here."))
			if !dest.existing.IsFolder() {
				e.Message += " " + i18n.T("Use --replace to replace its content.")
			}
			return nil, e
		}
		u.dataID = dest.existing.DataID
		return u, nil
	}
	if mediaType == "" {
		mediaType = mediaTypeOf(dest.name)
	}
	err = t.stone.Do(ctx, func(token string) error {
		var err error
		u.dataID, err = t.stone.Client.AddData(ctx, token, client.ProcessDataRequest{CircleID: t.room.ID, FolderID: dest.folderID, DataName: dest.name, MediaType: mediaType})
		return err
	})
	if err != nil {
		return nil, err
	}
	u.created = true
	return u, nil
}

// store sends the content of a prepared upload. The session was used just
// before, to prepare, so the content, which may come from a pipe and can't
// be sent twice, isn't repeated after a failure. A file registered for
// content that then failed is removed again.
func (c *command) store(ctx context.Context, u *upload, content io.Reader, length int64) (string, error) {
	// In a chamber the content is encrypted on its way (E2E.md).
	content, length, err := u.t.stone.Sealing(ctx, u.t.room.ID, content, length)
	var etag string
	if err == nil {
		etag, err = u.t.stone.Client.Upload(ctx, u.t.stone.Token(), u.dataID, content, length)
	}
	if err != nil && u.created {
		cleanup, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		_ = u.t.stone.Client.DeleteData(cleanup, u.t.stone.Token(), u.dataID)
	}
	return etag, err
}

// send uploads a file, or standard input, to one or more rooms.
func (c *command) send(args []string) error {
	fs := c.flags("send")
	name := fs.String("name", "", "")
	mediaType := fs.String("type", "", "")
	replace := fs.Bool("replace", false, "")
	parents := fs.Bool("parents", false, "")
	fs.BoolVar(parents, "p", false, "")
	share := fs.Bool("share", false, "")
	shareText := fs.String("message", "", "")
	fs.StringVar(shareText, "m", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) < 2 {
		return usage("Use 'tolk send <file|-> <[stone:]room[/folder/][name]> … [--name <name>] [--replace] [--share]'.")
	}
	if *shareText != "" {
		*share = true
	}
	if *mediaType != "" && !mediaTypePattern.MatchString(*mediaType) {
		return usage("%q is not a media type, like text/plain.", *mediaType)
	}
	source, destinations := pos[0], pos[1:]

	// What is sent: a file, which can be read again for each room, or a
	// stream, which can be read once.
	var file *os.File
	length := int64(-1)
	sourceName := *name
	if source == "-" {
		if in, ok := c.env.In.(*os.File); ok {
			if info, err := in.Stat(); err == nil && info.Mode().IsRegular() {
				// tolk send - < file: the length is known after all.
				if at, err := in.Seek(0, io.SeekCurrent); err == nil {
					file, length = in, info.Size()-at
				}
			}
		}
	} else {
		opened, err := os.Open(source)
		if err != nil {
			return exit.New(exit.NotFound, "%s", i18n.T("Cannot read %s: %v", source, rootError(err)))
		}
		defer opened.Close()
		info, err := opened.Stat()
		if err != nil {
			return exit.New(exit.Other, "%v", err)
		}
		if info.IsDir() {
			return usage("%s is a directory; send its files one by one.", source)
		}
		if info.Mode().IsRegular() {
			file, length = opened, info.Size()
		}
		if sourceName == "" {
			sourceName = filepath.Base(source)
		}
		if file == nil {
			// A named pipe or device: read once, like standard input.
			c.env.In = opened
		}
	}

	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()

	// With --share the file is posted in the room's chat, as Rune does.
	connections := newChats()
	defer connections.close()
	done := func(u *upload, etag string, size int64) (map[string]any, error) {
		c.note(i18n.T("Sent %s to %s (%s).", u.name, u.t.label, c.locale().Size(size)))
		out := map[string]any{"id": u.dataID, "name": u.name, "size": size, "etag": etag}
		if *share {
			if err := connections.share(ctx, u.t, u.dataID, *shareText); err != nil {
				return nil, exit.New(c.friendly(err).Code, "%s", i18n.T("The file is in the room, but sharing it in the chat failed: %s", c.friendly(err).Message))
			}
			out["shared"] = true
		}
		return out, nil
	}
	if file != nil {
		start, _ := file.Seek(0, io.SeekCurrent)
		return c.each(ctx, svc, destinations, func(t target) (any, error) {
			u, err := c.prepare(ctx, t, sourceName, *mediaType, *replace, *parents)
			if err != nil {
				return nil, err
			}
			if _, err := file.Seek(start, io.SeekStart); err != nil {
				return nil, exit.New(exit.Other, "%v", err)
			}
			bar := c.progress(t.label, length)
			etag, err := c.store(ctx, u, progressReader{io.LimitReader(file, length), bar}, length)
			bar.clear()
			if err != nil {
				return nil, err
			}
			return done(u, etag, length)
		})
	}

	// A stream: every room is prepared first, then the stream is read once
	// and sent to all of them at the same time.
	plan, err := c.resolveAll(ctx, svc, destinations)
	if err != nil {
		return err
	}
	outcomes := make([]outcome, len(plan))
	var uploads []*upload
	var slots []int // where each upload's outcome goes, to keep the order given
	for i, p := range plan {
		if p.failed != nil {
			outcomes[i] = *p.failed
			continue
		}
		u, err := c.prepare(ctx, p.target, sourceName, *mediaType, *replace, *parents)
		if err != nil {
			outcomes[i] = c.failure(p.target.label, err)
			continue
		}
		uploads, slots = append(uploads, u), append(slots, i)
	}
	if len(uploads) > 0 {
		bar := c.progress(i18n.T("Sending"), -1)
		etags, errs, size := c.fanOut(ctx, progressReader{c.env.In, bar}, uploads)
		bar.clear()
		for i, u := range uploads {
			if errs[i] != nil {
				outcomes[slots[i]] = c.failure(u.t.label, errs[i])
				continue
			}
			result, err := done(u, etags[i], size)
			if err != nil {
				outcomes[slots[i]] = c.failure(u.t.label, err)
				continue
			}
			outcomes[slots[i]] = outcome{Target: u.t.label, OK: true, Result: result}
		}
	}
	return c.finish(outcomes)
}

// fanOut reads a stream once and sends it to every prepared upload at the
// same time, through memory only. An upload that fails is dropped; the
// others go on. It returns each upload's ETag or error, and the bytes read.
func (c *command) fanOut(ctx context.Context, source io.Reader, uploads []*upload) ([]string, []error, int64) {
	etags := make([]string, len(uploads))
	errs := make([]error, len(uploads))
	writers := make([]*io.PipeWriter, len(uploads))
	var wait sync.WaitGroup
	for i, u := range uploads {
		reader, writer := io.Pipe()
		writers[i] = writer
		wait.Add(1)
		go func() {
			defer wait.Done()
			etags[i], errs[i] = c.store(ctx, u, reader, -1)
			// Whatever is still written is refused, so the source goes on
			// for the others.
			reader.CloseWithError(io.ErrClosedPipe)
		}()
	}
	var size int64
	buffer := make([]byte, 256<<10)
	var readErr error
	for {
		n, err := source.Read(buffer)
		if n > 0 {
			size += int64(n)
			for _, writer := range writers {
				_, _ = writer.Write(buffer[:n]) // a failed upload reports its own error
			}
		}
		if err != nil {
			if err != io.EOF {
				readErr = err
			}
			break
		}
		if ctx.Err() != nil {
			readErr = ctx.Err()
			break
		}
	}
	for _, writer := range writers {
		if readErr != nil {
			writer.CloseWithError(readErr)
		} else {
			writer.Close()
		}
	}
	wait.Wait()
	if readErr != nil {
		for i := range errs {
			if errs[i] == nil {
				errs[i] = exit.New(exit.Other, "%s", i18n.T("Reading the input failed: %v", readErr))
			}
		}
	}
	return etags, errs, size
}

func rootError(err error) error {
	var pathErr *os.PathError
	if errors.As(err, &pathErr) {
		return pathErr.Err
	}
	return err
}

// get downloads a file, to a local file or to standard output. A download
// to a file that was interrupted is continued by running it again.
func (c *command) get(args []string) error {
	fs := c.flags("get")
	output := fs.String("o", "", "")
	fs.StringVar(output, "out", "", "")
	force := fs.Bool("force", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk get <[stone:]room/path> [-o <file|folder|->]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	addresses, err := svc.Expand(pos)
	if err != nil {
		return err
	}
	if len(addresses) != 1 || addresses[0].AllRooms {
		return usage("Name one file, as [stone:]room/path.")
	}
	targets, err := c.targets(ctx, svc, addresses[0])
	if err != nil {
		return err
	}
	t := targets[0]
	entry, err := t.stone.File(ctx, t.room, t.path)
	if err != nil {
		return err
	}

	if *output == "-" {
		var content *client.Content
		err = t.stone.Do(ctx, func(token string) error {
			var err error
			content, err = t.stone.Client.Download(ctx, token, entry.DataID, 0, "")
			return err
		})
		if err != nil {
			return err
		}
		defer content.Body.Close()
		bar := c.progress(entry.DataName, content.Total)
		// Of a chamber, what arrives is decrypted on its way.
		readable, protected, err := t.stone.Opening(ctx, t.room.ID, progressReader{content.Body, bar})
		if err != nil {
			return err
		}
		if !protected {
			c.note(i18n.T("%s is not protected: it was written by an app which does not know chambers.", entry.DataName))
		}
		_, err = io.Copy(c.env.Out, readable)
		bar.clear()
		if err != nil {
			return &exit.Error{Code: exit.Network, Message: i18n.T("The download of %s was interrupted: %v", entry.DataName, err)}
		}
		return nil
	}

	path := *output
	if path == "" {
		path = safeLocalName(entry.DataName)
	} else if info, err := os.Stat(path); (err == nil && info.IsDir()) || strings.HasSuffix(path, string(os.PathSeparator)) || strings.HasSuffix(path, "/") {
		path = filepath.Join(path, safeLocalName(entry.DataName))
	}
	if _, err := os.Stat(path); err == nil && !*force {
		if !c.interactive() {
			return exit.New(exit.Usage, "%s", i18n.T("%s exists already. Use --force to replace it.", path))
		}
		ok, err := c.confirm(false, i18n.T("%s exists already. Replace it?", path))
		if err != nil {
			return err
		}
		if !ok {
			return exit.New(exit.Other, "%s", i18n.T("Nothing was downloaded."))
		}
	}

	// The unfinished download is kept beside the file, with the ETag of the
	// content it is a part of, so it is only continued with the same content.
	part, tag := path+".eds-part", path+".eds-part.etag"
	var from int64
	etag := ""
	if info, err := os.Stat(part); err == nil {
		if data, err := os.ReadFile(tag); err == nil && len(data) > 0 && info.Size() > 0 {
			from, etag = info.Size(), strings.TrimSpace(string(data))
		}
	}
	download := func() (*client.Content, error) {
		var content *client.Content
		err := t.stone.Do(ctx, func(token string) error {
			var err error
			content, err = t.stone.Client.Download(ctx, token, entry.DataID, from, etag)
			return err
		})
		return content, err
	}
	content, err := download()
	var e *exit.Error
	if errors.As(err, &e) && e.ReturnCode == 416 && from > 0 {
		// The part is as long as the content, or longer: start again.
		from, etag = 0, ""
		content, err = download()
	}
	if err != nil {
		return err
	}
	defer content.Body.Close()
	resumed := content.Partial && from > 0
	flags := os.O_WRONLY | os.O_CREATE | os.O_APPEND
	if !resumed {
		from = 0
		flags = os.O_WRONLY | os.O_CREATE | os.O_TRUNC
	}
	file, err := os.OpenFile(part, flags, 0o600)
	if err != nil {
		return exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", part, rootError(err)))
	}
	if err := os.WriteFile(tag, []byte(content.ETag+"\n"), 0o600); err != nil {
		file.Close()
		return exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", tag, rootError(err)))
	}
	total := content.Total
	bar := c.progress(entry.DataName, total)
	bar.done = from
	written, err := io.Copy(file, progressReader{content.Body, bar})
	bar.clear()
	closeErr := file.Close()
	if err == nil {
		err = closeErr
	}
	if err == nil && content.Length >= 0 && written != content.Length {
		err = io.ErrUnexpectedEOF
	}
	if err != nil {
		return &exit.Error{Code: exit.Network, Message: i18n.T("The download of %s was interrupted after %s: %v. Run the command again to continue.", entry.DataName, c.locale().Size(from+written), err)}
	}
	size := from + written
	// Of a chamber the part is what the stone stores, so a download can be
	// continued: it is decrypted now that it is whole.
	plain, protected, err := c.reveal(ctx, t, part, path)
	if err != nil {
		return err
	}
	if !protected {
		c.note(i18n.T("%s is not protected: it was written by an app which does not know chambers.", entry.DataName))
	}
	if plain >= 0 {
		size = plain
	} else if err := os.Rename(part, path); err != nil {
		return exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", path, rootError(err)))
	}
	_ = os.Remove(tag)
	if c.json() {
		return c.printJSON(map[string]any{"id": entry.DataID, "name": entry.DataName, "file": path, "size": size, "resumed": resumed})
	}
	if resumed {
		c.note(i18n.T("Downloaded %s to %s (%s, continued).", entry.DataName, path, c.locale().Size(size)))
	} else {
		c.note(i18n.T("Downloaded %s to %s (%s).", entry.DataName, path, c.locale().Size(size)))
	}
	return nil
}

func safeLocalName(name string) string { return service.LocalName(name) }

// reveal turns the complete part of a download into the file. In a room
// nothing is to do, and -1 is returned: the caller renames the part. Of a
// chamber the part is decrypted into the file, as a stream, and removed;
// the size of the file is returned. Content of a chamber which isn't
// protected is kept as it is, and told.
func (c *command) reveal(ctx context.Context, t target, part, path string) (size int64, protected bool, err error) {
	state, err := t.stone.RoomState(ctx, t.room.ID)
	if err != nil {
		return 0, false, err
	}
	if !state.Chamber() {
		return -1, true, nil
	}
	stored, err := os.Open(part)
	if err != nil {
		return 0, false, exit.New(exit.Other, "%s", i18n.T("Cannot read %s: %v", part, rootError(err)))
	}
	defer stored.Close()
	readable, protected, err := t.stone.Opening(ctx, t.room.ID, stored)
	if err != nil {
		return 0, false, err
	}
	if !protected {
		return -1, false, nil
	}
	temporary := path + ".eds-plain"
	out, err := os.OpenFile(temporary, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0o600)
	if err != nil {
		return 0, false, exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", temporary, rootError(err)))
	}
	size, err = io.Copy(out, readable)
	if closeErr := out.Close(); err == nil {
		err = closeErr
	}
	if err != nil {
		_ = os.Remove(temporary)
		return 0, false, exit.New(exit.Other, "%s", i18n.T("The file can't be decrypted: %v", err))
	}
	if err := os.Rename(temporary, path); err != nil {
		return 0, false, exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", path, rootError(err)))
	}
	_ = os.Remove(part)
	return size, true, nil
}

// cp copies a file to other rooms, also on other stones. The content goes
// from stone to stone through Tolk's memory, never its disk.
func (c *command) cp(args []string) error {
	fs := c.flags("cp")
	replace := fs.Bool("replace", false, "")
	parents := fs.Bool("parents", false, "")
	fs.BoolVar(parents, "p", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) < 2 {
		return usage("Use 'tolk cp <[stone:]room/path> <[stone:]room[/folder/][name]> … [--replace]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	sources, err := svc.Expand(pos[:1])
	if err != nil {
		return err
	}
	if len(sources) != 1 || sources[0].AllRooms {
		return usage("Name one file to copy, as [stone:]room/path.")
	}
	from, err := c.targets(ctx, svc, sources[0])
	if err != nil {
		return err
	}
	src := from[0]
	entry, err := src.stone.File(ctx, src.room, src.path)
	if err != nil {
		return err
	}
	return c.each(ctx, svc, pos[1:], func(t target) (any, error) {
		u, err := c.prepare(ctx, t, entry.DataName, entry.MediaType, *replace, *parents)
		if err != nil {
			return nil, err
		}
		var content *client.Content
		err = src.stone.Do(ctx, func(token string) error {
			var err error
			content, err = src.stone.Client.Download(ctx, token, entry.DataID, 0, "")
			return err
		})
		if err != nil {
			if u.created {
				_ = t.stone.Client.DeleteData(ctx, t.stone.Token(), u.dataID)
			}
			return nil, err
		}
		defer content.Body.Close()
		bar := c.progress(t.label, content.Length)
		// Out of a chamber the content is decrypted, into one encrypted: so a
		// copy between rooms, chambers and stones goes through Tolk.
		length := content.Length
		readable, protected, err := src.stone.Opening(ctx, src.room.ID, progressReader{content.Body, bar})
		if err == nil && !protected {
			err = exit.New(exit.NotPermitted, "%s", i18n.T("%s is not protected: it was written by an app which does not know chambers. Tolk doesn't copy it as part of a chamber; download it and send it where it should go.", entry.DataName))
		}
		if err != nil {
			if u.created {
				_ = t.stone.Client.DeleteData(ctx, t.stone.Token(), u.dataID)
			}
			return nil, err
		}
		if length >= 0 {
			length = src.stone.ShownSize(ctx, src.room.ID, length)
		}
		etag, err := c.store(ctx, u, readable, length)
		bar.clear()
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("Copied %s to %s (%s).", src.label, t.label, c.locale().Size(bar.done)))
		return map[string]any{"id": u.dataID, "name": u.name, "size": bar.done, "etag": etag}, nil
	})
}
