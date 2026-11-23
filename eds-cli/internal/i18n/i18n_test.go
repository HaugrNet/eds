package i18n

import (
	"go/ast"
	"go/parser"
	"go/token"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"testing"
)

// texts collects the English texts of Tolk: the first argument of every
// i18n.T(…) and usage(…) call in the sources, or of every i18n.Rune(…) call
// for the texts shared with Rune.
func texts(t *testing.T, function string) map[string]string {
	found := map[string]string{}
	root := filepath.Join("..", "..")
	err := filepath.WalkDir(root, func(path string, d fs.DirEntry, err error) error {
		if err != nil || d.IsDir() || !strings.HasSuffix(path, ".go") || strings.HasSuffix(path, "_test.go") {
			return err
		}
		file, err := parser.ParseFile(token.NewFileSet(), path, nil, 0)
		if err != nil {
			return err
		}
		ast.Inspect(file, func(n ast.Node) bool {
			call, ok := n.(*ast.CallExpr)
			if !ok || len(call.Args) == 0 {
				return true
			}
			name := ""
			switch fun := call.Fun.(type) {
			case *ast.SelectorExpr:
				if pkg, ok := fun.X.(*ast.Ident); ok && pkg.Name == "i18n" && fun.Sel.Name == function {
					name = function
				}
			case *ast.Ident:
				if (fun.Name == "usage" && function == "T") || (fun.Name == function && file.Name.Name == "i18n") {
					name = fun.Name
				}
			}
			if name == "" {
				return true
			}
			// usage(format, …) itself passes its format on; its callers are
			// checked. The message table passes its texts, which have their
			// own test.
			if id, ok := call.Args[0].(*ast.Ident); ok && id.Name == "format" {
				return true
			}
			if strings.HasSuffix(filepath.ToSlash(path), "internal/messages/messages.go") {
				return true
			}
			lit, ok := call.Args[0].(*ast.BasicLit)
			if !ok || lit.Kind != token.STRING {
				t.Errorf("%s: %s(…) must take a string literal, so the text can be translated", path, name)
				return true
			}
			text, err := strconv.Unquote(lit.Value)
			if err != nil {
				t.Errorf("%s: %v", path, err)
				return true
			}
			found[text] = path
			return true
		})
		return nil
	})
	if err != nil {
		t.Fatal(err)
	}
	return found
}

func TestGermanIsComplete(t *testing.T) {
	used := texts(t, "T")
	if len(used) < 20 {
		t.Fatalf("only %d texts found; the scan is broken", len(used))
	}
	de := Catalog("de")
	var missing []string
	for text := range used {
		if de[text] == "" {
			missing = append(missing, text)
		}
	}
	sort.Strings(missing)
	if dump := os.Getenv("TOLK_I18N_DUMP"); dump != "" {
		var b strings.Builder
		for _, text := range missing {
			b.WriteString(strconv.Quote(text) + "\n")
		}
		_ = os.WriteFile(dump, []byte(b.String()), 0o600)
	}
	for _, text := range missing {
		t.Errorf("no German for %q (%s)", text, used[text])
	}
	for text := range de {
		if _, ok := used[text]; !ok {
			t.Errorf("German text is no longer used: %q", text)
		}
	}
	// The same placeholders, in the same order, in both languages.
	for text, german := range de {
		if verbs(text) != verbs(german) {
			t.Errorf("placeholders differ: %q / %q", text, german)
		}
	}
}

// Every text Tolk takes from Rune must be in Rune's catalog.
func TestSharedTextsAreRunes(t *testing.T) {
	de := RuneCatalog("de")
	if len(de) < 100 {
		t.Fatalf("Rune's catalog has only %d texts", len(de))
	}
	for text, path := range texts(t, "Rune") {
		if de[text] == "" {
			t.Errorf("Rune has no text %q (%s)", text, path)
		}
	}
}

func TestRune(t *testing.T) {
	Set("de")
	defer Set("en")
	if got := Rune("The file is larger than the stone allows ({size}).", "size", "2 GB"); got != "Die Datei ist größer, als der Stein erlaubt (2 GB)." {
		t.Errorf("de: %q", got)
	}
	Set("en")
	if got := Rune("You can only read in this room."); got != "You can only read in this room." {
		t.Errorf("en: %q", got)
	}
}

func verbs(s string) string {
	var out []string
	for i := 0; i+1 < len(s); i++ {
		if s[i] == '%' {
			if s[i+1] != '%' {
				out = append(out, s[i:i+2])
			}
			i++
		}
	}
	return strings.Join(out, " ")
}

func TestSetAndT(t *testing.T) {
	catalogs["de"]["Hello %s"] = "Hallo %s"
	defer delete(catalogs["de"], "Hello %s")
	Set("de")
	if got := T("Hello %s", "Kim"); got != "Hallo Kim" {
		t.Errorf("de: %q", got)
	}
	Set("en")
	if got := T("Hello %s", "Kim"); got != "Hello Kim" {
		t.Errorf("en: %q", got)
	}
	Set("xx")
	if Language() != "en" {
		t.Errorf("unknown language should give English, got %s", Language())
	}
	t.Setenv("LC_ALL", "")
	t.Setenv("LC_MESSAGES", "de_DE.UTF-8")
	Set("auto")
	if Language() != "de" {
		t.Errorf("auto with LC_MESSAGES=de_DE: %s", Language())
	}
	Set("en")
}
