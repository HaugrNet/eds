package commands

import (
	"bytes"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// object decodes the JSON object a command printed.
func object(t *testing.T, out string) map[string]any {
	t.Helper()
	var value map[string]any
	if err := json.Unmarshal([]byte(out), &value); err != nil {
		t.Fatalf("%v in %s", err, out)
	}
	return value
}

// texts are the texts of the history which 'tolk chat --plain' printed.
func texts(t *testing.T, out string) []string {
	t.Helper()
	var list []string
	for _, event := range events(t, out) {
		if event.History && event.Text != nil {
			list = append(list, *event.Text)
		}
	}
	return list
}

// The flow of a chamber from the command line (E2E.md, phase E5): made by
// one, joined by a link with nobody online, files of several chunks both
// ways and through a pipe, a member added and repaired, one removed, and a
// further device with the recovery key.
func TestChamberFlow(t *testing.T) {
	work, _ := world(t)
	dir := t.TempDir()

	// A chamber: the account gets keys with it, and a recovery key.
	made := object(t, must(t, "", "room", "new", "work:Vault", "--chamber", "--output", "json"))
	recovery, _ := made["recoveryKey"].(string)
	if made["chamber"] != true || e2e.ParseRecoveryKey(recovery) == nil {
		t.Fatalf("the chamber: %v", made)
	}
	if features, _ := made["features"].([]any); len(features) != 1 || features[0] != "chat" {
		t.Errorf("its features: %v", made["features"])
	}
	keys := object(t, must(t, "", "key", "show", "work", "--output", "json"))
	fingerprint, _ := keys["fingerprint"].(string)
	if keys["keys"] != "ready" || len(strings.Fields(fingerprint)) != 8 {
		t.Errorf("the keys: %v", keys)
	}
	if again := strings.TrimSpace(must(t, "", "key", "recovery", "work")); again != recovery {
		t.Errorf("the recovery key is shown again as %q, was %q", again, recovery)
	}
	if second := object(t, must(t, "", "room", "new", "work:Cellar", "--chamber", "--features", "none", "--output", "json")); second["recoveryKey"] != nil {
		t.Errorf("a second chamber made new keys: %v", second)
	}

	// The chat: the stone holds what it can't read.
	must(t, "", "chat", "--plain", "--room", "work:Vault", "-m", "secret words")
	stored := work.Stored("Vault", "chat", "message")
	if len(stored) != 1 || !e2e.IsProtected(stored[0]) || bytes.Contains(stored[0], []byte("secret")) {
		t.Fatalf("the stone holds %q", stored)
	}
	if got := texts(t, must(t, "", "chat", "--plain", "--room", "work:Vault", "--history", "5")); len(got) != 1 || got[0] != "secret words" {
		t.Errorf("the history: %v", got)
	}

	// A file of several chunks goes in encrypted, and comes back unchanged.
	content := make([]byte, 3*65536+4321)
	for i := range content {
		content[i] = byte(i*31 + 7)
	}
	source := filepath.Join(dir, "big.bin")
	if err := os.WriteFile(source, content, 0o600); err != nil {
		t.Fatal(err)
	}
	must(t, "", "send", source, "work:Vault/")
	held, ok := work.File("Vault", "big.bin")
	if !ok || !e2e.IsProtected(held) || int64(len(held)) != e2e.EncryptedLength(int64(len(content))) {
		t.Fatalf("the stone holds %d bytes, protected: %v", len(held), e2e.IsProtected(held))
	}
	target := filepath.Join(dir, "back.bin")
	got := object(t, must(t, "", "get", "work:Vault/big.bin", "-o", target, "--output", "json"))
	if back, err := os.ReadFile(target); err != nil || !bytes.Equal(back, content) || got["size"] != float64(len(content)) {
		t.Errorf("the file came back changed: %v, %v", err, got)
	}
	if out := must(t, "", "get", "work:Vault/big.bin", "-o", "-"); out != string(content) {
		t.Error("the file came back changed through a pipe")
	}
	// From a pipe, without a length, and an empty one.
	must(t, "through a pipe", "send", "-", "work:Vault/piped.txt")
	if held, _ := work.File("Vault", "piped.txt"); !e2e.IsProtected(held) || len(held) != 54+len("through a pipe")+16 {
		t.Errorf("a pipe into the chamber: %d bytes", len(held))
	}
	if out := must(t, "", "get", "work:Vault/piped.txt", "-o", "-"); out != "through a pipe" {
		t.Errorf("the piped file: %q", out)
	}
	must(t, "", "send", "-", "work:Vault/empty.txt")
	if held, _ := work.File("Vault", "empty.txt"); len(held) != 70 {
		t.Errorf("an empty file is a header and a tag, got %d bytes", len(held))
	}
	if out := must(t, "", "get", "work:Vault/empty.txt", "-o", "-"); out != "" {
		t.Errorf("the empty file: %q", out)
	}
	// A copy out of the chamber is readable to the stone, one into it isn't.
	must(t, "", "cp", "work:Vault/big.bin", "work:Projects/")
	if plain, _ := work.File("Projects", "big.bin"); !bytes.Equal(plain, content) {
		t.Error("the copy in the room isn't the file")
	}
	must(t, "", "cp", "work:Projects/big.bin", "work:Vault/copy.bin")
	if again, _ := work.File("Vault", "copy.bin"); !e2e.IsProtected(again) || bytes.Equal(again, held) {
		t.Error("the copy in the chamber isn't encrypted anew")
	}
	if out := must(t, "", "get", "work:Vault/copy.bin", "-o", "-"); out != string(content) {
		t.Error("the copy came back changed")
	}

	// An invitation by link: it carries the invite secret, and accepting it
	// needs nobody else.
	invited := object(t, must(t, "", "invite", "carol", "--room", "work:Vault", "--output", "json"))
	link, _ := invited["link"].(string)
	if read := e2e.ParseInvitationFragment(link[strings.Index(link, "#invite="):]); read == nil || len(read.Secret) != 32 || invited["chamber"] != true {
		t.Fatalf("the invitation: %v", invited)
	}
	if e2e.SecretKind(work.Secret("Vault", "carol")) != "invite" {
		t.Errorf("carol's secret waits as %q", work.Secret("Vault", "carol"))
	}
	joined := object(t, must(t, "carol's long passphrase 2026\n", "accept", link, "--stone", "carol", "--passphrase-stdin", "--output", "json"))
	if key, _ := joined["recoveryKey"].(string); e2e.ParseRecoveryKey(key) == nil {
		t.Fatalf("carol got no recovery key: %v", joined)
	}
	if e2e.SecretKind(work.Secret("Vault", "carol")) != "key" {
		t.Errorf("carol's secret is now %q", work.Secret("Vault", "carol"))
	}
	if got := texts(t, must(t, "", "chat", "--plain", "--room", "carol:Vault", "--history", "5")); len(got) != 1 || got[0] != "secret words" {
		t.Errorf("carol reads %v", got)
	}
	must(t, "", "chat", "--plain", "--room", "carol:Vault", "-m", "carol here")
	if out := must(t, "", "get", "carol:Vault/piped.txt", "-o", "-"); out != "through a pipe" {
		t.Errorf("carol reads the file as %q", out)
	}

	// Someone without keys is added: they wait, get keys, and an
	// administrator who looks at the members repairs.
	must(t, "birgit secret\n", "register", "stone", "birgit", "--url", work.URL(), "--account", "birgit", "--passphrase-stdin")
	_, _, errOut := run("", "add", "birgit", "--room", "work:Vault", "--access", "read")
	if !strings.Contains(errOut, "has no keys for chambers yet") {
		t.Errorf("adding someone without keys: %s", errOut)
	}
	if code, _, errOut := run("", "chat", "--plain", "--room", "birgit:Vault", "--history", "5"); code != exit.Auth || !strings.Contains(errOut, "no key to this chamber") {
		t.Errorf("birgit without keys: code %d, %s", code, errOut)
	}
	must(t, "", "key", "create", "birgit")
	members := results(t, must(t, "", "members", "--room", "work:Vault", "--output", "json"))
	states := map[string]any{}
	for _, m := range members[0].Result.(map[string]any)["members"].([]any) {
		member := m.(map[string]any)
		states[member["name"].(string)] = member["key"]
	}
	if states["birgit"] != "current" || states["carol"] != "current" || states["admin"] != "current" {
		t.Errorf("after the repair: %v", states)
	}
	if got := texts(t, must(t, "", "chat", "--plain", "--room", "birgit:Vault", "--history", "5")); len(got) != 2 {
		t.Errorf("birgit reads %v", got)
	}

	// A member is removed: the chamber gets a new key, and what was written
	// before stays readable.
	before := work.Secret("Vault", "birgit")
	_, _, errOut = run("", "remove", "carol", "--room", "work:Vault")
	if !strings.Contains(errOut, "new key") || work.Secret("Vault", "birgit") == before || e2e.SecretVersion(work.Secret("Vault", "birgit")) != 2 {
		t.Errorf("removing carol: %s", errOut)
	}
	must(t, "", "chat", "--plain", "--room", "work:Vault", "-m", "after carol left")
	if got := texts(t, must(t, "", "chat", "--plain", "--room", "birgit:Vault", "--history", "5")); len(got) != 3 || got[2] != "after carol left" {
		t.Errorf("birgit reads old and new: %v", got)
	}
	if out := must(t, "", "get", "birgit:Vault/big.bin", "-o", "-"); out != string(content) {
		t.Error("a file of the earlier key came back changed")
	}
	if code, _, _ := run("", "chat", "--plain", "--room", "carol:Vault", "--history", "5"); code == exit.OK {
		t.Error("carol, removed, still reads the chamber")
	}

	// A further device: the same account without the account key.
	must(t, "admin secret\n", "register", "stone", "second", "--url", work.URL(), "--account", "admin", "--passphrase-stdin")
	if keys := object(t, must(t, "", "key", "show", "second", "--output", "json")); keys["keys"] != "locked" || keys["fingerprint"] != fingerprint {
		t.Errorf("on a further device: %v", keys)
	}
	if code, _, errOut := run("", "send", source, "second:Vault/again.bin"); code == exit.OK || !strings.Contains(errOut, "tolk key unlock") {
		t.Errorf("writing without the keys: code %d, %s", code, errOut)
	}
	if code, _, _ := run("not a key\n", "key", "unlock", "second", "--key-stdin"); code == exit.OK {
		t.Error("something else was taken as the recovery key")
	}
	must(t, strings.ToLower(strings.ReplaceAll(recovery, "-", " "))+"\n", "key", "unlock", "second", "--key-stdin")
	if got := texts(t, must(t, "", "chat", "--plain", "--room", "second:Vault", "--history", "5")); len(got) != 3 {
		t.Errorf("the further device reads %v", got)
	}
	// And by the link another device shows.
	link = strings.TrimSpace(must(t, "", "key", "link", "work", "--no-qr"))
	if !strings.Contains(link, "#device=") {
		t.Fatalf("the device link: %q", link)
	}
	must(t, "admin secret\n", "register", "stone", "third", "--url", work.URL(), "--account", "admin", "--passphrase-stdin")
	must(t, link+"\n", "key", "unlock", "third", "--key-stdin")
	if keys := object(t, must(t, "", "key", "show", "third", "--output", "json")); keys["keys"] != "ready" {
		t.Errorf("after the link: %v", keys)
	}

	// A stone which presents the chamber as a room is noticed: nothing is
	// written to it.
	work.Strip("Vault")
	if code, _, errOut := run("", "chat", "--plain", "--room", "work:Vault", "-m", "in the clear?"); code == exit.OK || !strings.Contains(errOut, "was a chamber") {
		t.Errorf("a stripped chamber: code %d, %s", code, errOut)
	}
	if code, _, errOut := run("", "send", source, "work:Vault/clear.bin"); code == exit.OK || !strings.Contains(errOut, "was a chamber") {
		t.Errorf("a file into a stripped chamber: code %d, %s", code, errOut)
	}
}

// Content of a chamber which no app that knows chambers wrote is told as
// such, and not copied as part of it.
func TestUnprotectedContentInAChamber(t *testing.T) {
	work, _ := world(t)
	must(t, "", "room", "new", "work:Vault", "--chamber")
	// Another app writes into the chamber as it would into a room.
	work.PutFile("Vault", "plain.txt", []byte("written in the clear"))
	code, out, errOut := run("", "get", "work:Vault/plain.txt", "-o", "-")
	if code != exit.OK || out != "written in the clear" || !strings.Contains(errOut, "not protected") {
		t.Errorf("unprotected content: code %d, %q, %s", code, out, errOut)
	}
	if code, _, errOut := run("", "cp", "work:Vault/plain.txt", "work:Vault/copy.txt"); code == exit.OK || !strings.Contains(errOut, "not protected") {
		t.Errorf("copying unprotected content: code %d, %s", code, errOut)
	}
}
