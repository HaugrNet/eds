package address

import (
	"reflect"
	"testing"
)

func TestParse(t *testing.T) {
	cases := []struct {
		in   string
		want Address
	}{
		{"work:finance/reports/q3.pdf", Address{Stone: "work", Room: "finance", Path: []string{"reports", "q3.pdf"}}},
		{"finance/reports/", Address{Room: "finance", Path: []string{"reports"}, Dir: true}},
		{"work:general", Address{Stone: "work", Room: "general", Dir: true}},
		{"work:*", Address{Stone: "work", Room: "*", AllRooms: true, Dir: true}},
		{`work:a\:b/c\/d`, Address{Stone: "work", Room: "a:b", Path: []string{"c/d"}}},
		{`home:5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a/x`, Address{Stone: "home", Room: "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a", Path: []string{"x"}}},
	}
	for _, c := range cases {
		got, err := Parse(c.in)
		if err != nil {
			t.Errorf("Parse(%q): %v", c.in, err)
			continue
		}
		if !reflect.DeepEqual(got, c.want) {
			t.Errorf("Parse(%q) = %+v, want %+v", c.in, got, c.want)
		}
		back, err := Parse(got.String())
		if err != nil || !reflect.DeepEqual(back, got) {
			t.Errorf("round trip of %q through %q failed: %+v, %v", c.in, got.String(), back, err)
		}
	}
}

func TestParseErrors(t *testing.T) {
	for _, in := range []string{"", ":room", "work:", "a:b:c", "work:room//x", "@group", "work:*/x"} {
		if _, err := Parse(in); err == nil {
			t.Errorf("Parse(%q) should fail", in)
		}
	}
}

func TestIsGroup(t *testing.T) {
	if name, ok := IsGroup("@announce"); !ok || name != "announce" {
		t.Errorf("IsGroup(@announce) = %q, %v", name, ok)
	}
	if _, ok := IsGroup("work:general"); ok {
		t.Error("work:general is not a group")
	}
}
