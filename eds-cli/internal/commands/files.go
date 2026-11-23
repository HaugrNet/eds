package commands

import (
	"context"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// entryJSON is a file or folder in machine-readable output.
func entryJSON(e service.Entry) map[string]any {
	out := map[string]any{"id": e.DataID, "name": e.DataName, "type": "file", "added": iso(e.Added)}
	if e.IsFolder() {
		out["type"] = "folder"
	} else {
		out["size"] = e.Size
		out["mediaType"] = e.MediaType
	}
	return out
}

// several reports whether the arguments may name more than one room, so
// each room's output gets a heading.
func several(args []string) bool {
	return len(args) > 1 || (len(args) == 1 && (strings.HasPrefix(args[0], "@") || strings.HasSuffix(args[0], "*")))
}

// ls lists the files and folders of rooms and folders.
func (c *command) ls(args []string) error {
	pos, err := parse(c.flags("ls"), args)
	if err != nil {
		return err
	}
	if len(pos) == 0 {
		return usage("Use 'tolk ls <[stone:]room[/folder]>'. 'tolk rooms' lists the rooms.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	headings := several(pos)
	return c.each(ctx, svc, pos, func(t target) (any, error) {
		var entries []service.Entry
		if len(t.path) == 0 {
			entries, err = t.stone.List(ctx, t.room, "")
		} else {
			var place *service.Place
			if place, err = t.stone.Find(ctx, t.room, t.path); err == nil {
				switch {
				case place.Entry == nil:
					err = exit.New(exit.NotFound, "%s", i18n.T("There is no %q in the room %q.", strings.Join(t.path, "/"), t.room.Name))
				case place.Entry.IsFolder():
					entries, err = t.stone.List(ctx, t.room, place.Entry.DataID)
				default:
					entries = []service.Entry{*place.Entry}
				}
			}
		}
		if err != nil {
			return nil, err
		}
		// Of a chamber's file the size is that of its content, not of what
		// the stone stores, which is a little more.
		for i := range entries {
			if !entries[i].IsFolder() {
				entries[i].Size = t.stone.ShownSize(ctx, t.room.ID, entries[i].Size)
			}
		}
		out := []map[string]any{}
		for _, e := range entries {
			out = append(out, entryJSON(e))
		}
		if !c.json() {
			if headings {
				c.say("%s:", t.label)
			}
			locale := c.locale()
			width := 1
			for _, e := range entries {
				if !e.IsFolder() {
					width = max(width, len(locale.Size(e.Size)))
				}
			}
			for _, e := range entries {
				size, name := "", e.DataName
				if e.IsFolder() {
					name += "/"
				} else {
					size = locale.Size(e.Size)
				}
				c.say("%*s  %s  %s", width, size, c.when(e.Added), name)
			}
		}
		return map[string]any{"entries": out}, nil
	})
}

// mkdir makes folders.
func (c *command) mkdir(args []string) error {
	fs := c.flags("mkdir")
	parents := fs.Bool("parents", false, "")
	fs.BoolVar(parents, "p", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) == 0 {
		return usage("Use 'tolk mkdir <[stone:]room/folder> [-p]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, pos, func(t target) (any, error) {
		if len(t.path) == 0 {
			return nil, usage("Name the folder to make, as [stone:]room/folder.")
		}
		if err := service.CheckWrite(t.room); err != nil {
			return nil, err
		}
		var id string
		if *parents {
			id, err = t.stone.MakeFolders(ctx, t.room, t.path)
		} else {
			var place *service.Place
			if place, err = t.stone.Find(ctx, t.room, t.path); err == nil {
				if place.Entry != nil {
					return nil, exit.New(exit.Usage, "%s", i18n.Rune("A file or folder with this name already exists here."))
				}
				if err = service.CheckName(place.Name); err == nil {
					err = t.stone.Do(ctx, func(token string) error {
						var err error
						id, err = t.stone.Client.AddData(ctx, token, client.ProcessDataRequest{CircleID: t.room.ID, FolderID: place.FolderID, DataName: place.Name, TypeName: client.TypeFolder})
						return err
					})
				}
			}
		}
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("Made the folder %s.", t.label))
		return map[string]any{"id": id}, nil
	})
}

// existing finds the entry an address names, which must exist.
func existing(ctx context.Context, t target) (*service.Entry, error) {
	if len(t.path) == 0 {
		return nil, usage("%q names a room; name a file or folder in it.", t.label)
	}
	place, err := t.stone.Find(ctx, t.room, t.path)
	if err != nil {
		return nil, err
	}
	if place.Entry == nil {
		return nil, exit.New(exit.NotFound, "%s", i18n.T("There is no %q in the room %q.", strings.Join(t.path, "/"), t.room.Name))
	}
	return place.Entry, nil
}

// rename gives a file or folder a new name.
func (c *command) rename(args []string) error {
	pos, err := parse(c.flags("rename"), args)
	if err != nil {
		return err
	}
	if len(pos) != 2 {
		return usage("Use 'tolk rename <[stone:]room/path> <new name>'.")
	}
	if err := service.CheckName(pos[1]); err != nil {
		return err
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, pos[:1], func(t target) (any, error) {
		if err := service.CheckWrite(t.room); err != nil {
			return nil, err
		}
		entry, err := existing(ctx, t)
		if err != nil {
			return nil, err
		}
		err = t.stone.Do(ctx, func(token string) error {
			return t.stone.Client.UpdateData(ctx, token, entry.DataID, client.ProcessDataRequest{DataName: pos[1]})
		})
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("Renamed %s to %q.", t.label, pos[1]))
		return map[string]any{"id": entry.DataID, "name": pos[1]}, nil
	})
}

// rm deletes files and empty folders.
func (c *command) rm(args []string) error {
	fs := c.flags("rm")
	yes := fs.Bool("yes", false, "")
	fs.BoolVar(yes, "y", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) == 0 {
		return usage("Use 'tolk rm <[stone:]room/path> …'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, pos, func(t target) (any, error) {
		if err := service.CheckWrite(t.room); err != nil {
			return nil, err
		}
		entry, err := existing(ctx, t)
		if err != nil {
			return nil, err
		}
		question := i18n.Rune("Delete \"{name}\"? It is deleted for everyone in the room, and can't be restored.", "name", entry.DataName)
		if entry.IsFolder() {
			question = i18n.Rune("Delete the folder \"{name}\"? It is deleted for everyone in the room, and can't be restored.", "name", entry.DataName)
		}
		ok, err := c.confirm(*yes, question)
		if err != nil {
			return nil, err
		}
		if !ok {
			return nil, exit.New(exit.Other, "%s", i18n.T("Nothing was deleted."))
		}
		err = t.stone.Do(ctx, func(token string) error { return t.stone.Client.DeleteData(ctx, token, entry.DataID) })
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("Deleted %s.", t.label))
		return map[string]any{"id": entry.DataID, "deleted": true}, nil
	})
}

// destination is where a file goes: a folder of a room, and its name there.
type destination struct {
	folderID string
	name     string
	existing *service.Entry // a file that already has that name there
}

// resolveDestination works out where an address puts a file with the given
// source name. An address ending in a slash, or naming a room or an
// existing folder, is the folder to put it in under its own name; anything
// else is the folder and the new name. With parents, missing folders are
// made.
func resolveDestination(ctx context.Context, t target, sourceName string, parents bool) (*destination, error) {
	dest := &destination{name: sourceName}
	folderPath := t.path
	if !t.dir && len(t.path) > 0 {
		place, err := t.stone.Find(ctx, t.room, t.path)
		if err != nil && !parents {
			return nil, err
		}
		if err == nil && place.Entry != nil && place.Entry.IsFolder() {
			dest.folderID = place.Entry.DataID
			folderPath = nil
		} else {
			dest.name = t.path[len(t.path)-1]
			folderPath = t.path[:len(t.path)-1]
		}
	}
	if len(folderPath) > 0 {
		var err error
		if parents {
			dest.folderID, err = t.stone.MakeFolders(ctx, t.room, folderPath)
		} else {
			dest.folderID, err = t.stone.Folder(ctx, t.room, folderPath)
		}
		if err != nil {
			return nil, err
		}
	}
	if dest.name == "" {
		return nil, usage("Give the file a name: end the address with it, or use --name.")
	}
	if err := service.CheckName(dest.name); err != nil {
		return nil, err
	}
	entries, err := t.stone.List(ctx, t.room, dest.folderID)
	if err != nil {
		return nil, err
	}
	for i := range entries {
		if entries[i].DataName == dest.name {
			dest.existing = &entries[i]
		}
	}
	return dest, nil
}

// mv moves a file to another folder or room of the same stone, and may
// rename it on the way.
func (c *command) mv(args []string) error {
	pos, err := parse(c.flags("mv"), args)
	if err != nil {
		return err
	}
	if len(pos) != 2 {
		return usage("Use 'tolk mv <[stone:]room/path> <[stone:]room[/folder/]>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	targets, err := svc.Expand(pos[1:])
	if err != nil {
		return err
	}
	if len(targets) != 1 || targets[0].AllRooms {
		return usage("Move to one place; to have a file in several rooms, use 'tolk cp'.")
	}
	return c.each(ctx, svc, pos[:1], func(t target) (any, error) {
		if err := service.CheckWrite(t.room); err != nil {
			return nil, err
		}
		entry, err := existing(ctx, t)
		if err != nil {
			return nil, err
		}
		to, err := c.targets(ctx, svc, targets[0])
		if err != nil {
			return nil, err
		}
		if to[0].stone != t.stone {
			return nil, usage("Files are moved within a stone. To another stone, use 'tolk cp' and then 'tolk rm'.")
		}
		if err := service.CheckWrite(to[0].room); err != nil {
			return nil, err
		}
		dest, err := resolveDestination(ctx, to[0], entry.DataName, false)
		if err != nil {
			return nil, err
		}
		if dest.existing != nil && dest.existing.DataID != entry.DataID {
			return nil, exit.New(exit.Usage, "%s", i18n.Rune("A file or folder with this name already exists there."))
		}
		id := entry.DataID
		sameRoom := to[0].room.ID == t.room.ID
		// The top level's own ID is the folder of what is at the top level.
		folderID := dest.folderID
		if folderID == "" && sameRoom {
			top, err := t.stone.List(ctx, t.room, "")
			if err != nil {
				return nil, err
			}
			if len(top) > 0 {
				folderID = top[0].FolderID
			}
		}
		err = t.stone.Do(ctx, func(token string) error {
			if !sameRoom {
				var err error
				id, err = t.stone.Client.MoveData(ctx, token, entry.DataID, client.ProcessDataRequest{TargetCircleID: to[0].room.ID, TargetFolderID: dest.folderID})
				return err
			}
			if folderID != "" && folderID != entry.FolderID {
				return t.stone.Client.UpdateData(ctx, token, entry.DataID, client.ProcessDataRequest{FolderID: folderID})
			}
			return nil
		})
		if err != nil {
			return nil, err
		}
		if id == "" {
			id = entry.DataID
		}
		if dest.name != entry.DataName {
			err = t.stone.Do(ctx, func(token string) error {
				return t.stone.Client.UpdateData(ctx, token, id, client.ProcessDataRequest{DataName: dest.name})
			})
			if err != nil {
				return nil, err
			}
		}
		c.note(i18n.T("Moved %s to %s.", t.label, to[0].label))
		return map[string]any{"id": id, "name": dest.name, "room": to[0].room.Name}, nil
	})
}
