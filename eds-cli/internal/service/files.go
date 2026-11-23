package service

import (
	"context"
	"sort"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// Entry is a file or folder in a room.
type Entry = client.Metadata

// List returns the entries of a folder, or of the room's top level for an
// empty folder ID: folders first, then by name.
func (st *Stone) List(ctx context.Context, room *Room, folderID string) ([]Entry, error) {
	var entries []Entry
	err := st.Do(ctx, func(token string) error {
		var err error
		entries, err = st.Client.List(ctx, token, room.ID, folderID)
		return err
	})
	if err != nil {
		return nil, err
	}
	sort.SliceStable(entries, func(i, j int) bool {
		if entries[i].IsFolder() != entries[j].IsFolder() {
			return entries[i].IsFolder()
		}
		return strings.ToLower(entries[i].DataName) < strings.ToLower(entries[j].DataName)
	})
	return entries, nil
}

// Place is where a path leads: the folder it is in, and the entry itself if
// it exists.
type Place struct {
	FolderID string // the folder the last name is in; empty for the top level
	Name     string // the last name of the path; empty for the room itself
	Entry    *Entry // nil when nothing has that name
}

// Find follows a path below a room. Each name is looked up among the
// entries of the folder before it, by name first and then by ID. Folders on
// the way must exist; the last name may be missing.
func (st *Stone) Find(ctx context.Context, room *Room, path []string) (*Place, error) {
	place := &Place{}
	for i, name := range path {
		entries, err := st.List(ctx, room, place.FolderID)
		if err != nil {
			return nil, err
		}
		entry := pick(entries, name)
		if i == len(path)-1 {
			place.Name, place.Entry = name, entry
			return place, nil
		}
		if entry == nil {
			return nil, exit.New(exit.NotFound, "%s", i18n.T("There is no folder %q in %s.", name, where(room, path[:i])))
		}
		if !entry.IsFolder() {
			return nil, exit.New(exit.Usage, "%s", i18n.T("%q in %s is a file, not a folder.", name, where(room, path[:i])))
		}
		place.FolderID = entry.DataID
	}
	return place, nil
}

// Folder follows a path that must be a folder (or the room itself), and
// returns its ID; empty for the top level.
func (st *Stone) Folder(ctx context.Context, room *Room, path []string) (string, error) {
	if len(path) == 0 {
		return "", nil
	}
	place, err := st.Find(ctx, room, path)
	if err != nil {
		return "", err
	}
	if place.Entry == nil {
		return "", exit.New(exit.NotFound, "%s", i18n.T("There is no folder %q in %s.", place.Name, where(room, path[:len(path)-1])))
	}
	if !place.Entry.IsFolder() {
		return "", exit.New(exit.Usage, "%s", i18n.T("%q in %s is a file, not a folder.", place.Name, where(room, path[:len(path)-1])))
	}
	return place.Entry.DataID, nil
}

// File follows a path that must be an existing file.
func (st *Stone) File(ctx context.Context, room *Room, path []string) (*Entry, error) {
	if len(path) == 0 {
		return nil, exit.New(exit.Usage, "%s", i18n.T("Name a file in the room %q.", room.Name))
	}
	place, err := st.Find(ctx, room, path)
	if err != nil {
		return nil, err
	}
	if place.Entry == nil {
		return nil, exit.New(exit.NotFound, "%s", i18n.T("There is no %q in %s.", place.Name, where(room, path[:len(path)-1])))
	}
	if place.Entry.IsFolder() {
		return nil, exit.New(exit.Usage, "%s", i18n.T("%q in %s is a folder, not a file.", place.Name, where(room, path[:len(path)-1])))
	}
	return place.Entry, nil
}

// MakeFolders makes the folders of a path that don't exist yet, and returns
// the ID of the last one.
func (st *Stone) MakeFolders(ctx context.Context, room *Room, path []string) (string, error) {
	folderID := ""
	for i, name := range path {
		entries, err := st.List(ctx, room, folderID)
		if err != nil {
			return "", err
		}
		if entry := pick(entries, name); entry != nil {
			if !entry.IsFolder() {
				return "", exit.New(exit.Usage, "%s", i18n.T("%q in %s is a file, not a folder.", name, where(room, path[:i])))
			}
			folderID = entry.DataID
			continue
		}
		if err := CheckName(name); err != nil {
			return "", err
		}
		parent := folderID
		err = st.Do(ctx, func(token string) error {
			var err error
			folderID, err = st.Client.AddData(ctx, token, client.ProcessDataRequest{CircleID: room.ID, FolderID: parent, DataName: name, TypeName: client.TypeFolder})
			return err
		})
		if err != nil {
			return "", err
		}
	}
	return folderID, nil
}

// CheckName refuses names Rune refuses: empty ones, and those starting with
// a dot, which are hidden.
func CheckName(name string) error {
	if strings.TrimSpace(name) == "" {
		return exit.New(exit.Usage, "%s", i18n.T("The name must not be empty."))
	}
	if strings.HasPrefix(name, ".") {
		return exit.New(exit.Usage, "%s", i18n.Rune("Names starting with a dot are hidden in Rune, so they aren't allowed here."))
	}
	return nil
}

// CheckWrite refuses early what the stone would refuse: writing in a room
// where the account can only read.
func CheckWrite(room *Room) error {
	if room.In() && !room.CanWrite() {
		return &exit.Error{Code: exit.NotPermitted, Message: i18n.Rune("You can only read in this room.")}
	}
	return nil
}

func pick(entries []Entry, name string) *Entry {
	for i := range entries {
		if entries[i].DataName == name {
			return &entries[i]
		}
	}
	for i := range entries {
		if entries[i].DataID == name {
			return &entries[i]
		}
	}
	return nil
}

// where names a folder for a message: the room, and the path below it.
func where(room *Room, path []string) string {
	if len(path) == 0 {
		return room.Name
	}
	return room.Name + "/" + strings.Join(path, "/")
}
