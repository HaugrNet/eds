// Package exit defines Tolk's exit codes and the error that carries one.
package exit

import "fmt"

// Exit codes (TOLK.md, "Behaviour").
const (
	OK           = 0
	Other        = 1 // also: the stone is locked or not ready
	Usage        = 2
	Auth         = 3
	NotFound     = 4
	Network      = 5 // also: the stone is unreachable
	NotPermitted = 6
)

// Error is an error with an exit code. ReturnCode and ReturnMessage are set
// when a stone answered with them.
type Error struct {
	Code          int
	Message       string
	ReturnCode    int
	ReturnMessage string
}

func (e *Error) Error() string { return e.Message }

// New makes an Error with a formatted message.
func New(code int, format string, args ...any) *Error {
	return &Error{Code: code, Message: fmt.Sprintf(format, args...)}
}

// FromReturnCode maps a stone's return code to an exit code.
func FromReturnCode(returnCode int) int {
	switch returnCode {
	case 200:
		return OK
	case 401:
		return NotPermitted
	case 403:
		return Auth
	case 404:
		return NotFound
	default:
		return Other
	}
}
