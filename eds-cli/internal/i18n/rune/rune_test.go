package rune

import (
	"bytes"
	"errors"
	"os"
	"testing"
)

// The copies must be those of Rune's sources. Outside the repository, where
// the sources aren't there, the test is skipped.
func TestCopiesMatchRune(t *testing.T) {
	for name, source := range Sources {
		want, err := os.ReadFile(source)
		if errors.Is(err, os.ErrNotExist) {
			t.Skipf("Rune's sources aren't here (%s)", source)
		}
		if err != nil {
			t.Fatal(err)
		}
		if Embedded[name] == nil || !bytes.Equal(want, *Embedded[name]) {
			t.Errorf("%s differs from %s; run 'go generate ./...'", name, source)
		}
	}
}
