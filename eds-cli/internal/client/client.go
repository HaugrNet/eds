// Package client is Tolk's thin, hand-written client of the EDS REST API, on
// net/http and encoding/json. A test checks every operation and field it
// uses against the contract (eds-api/src/main/resources/openapi.json).
package client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// Envelope is the part every EDS response has.
type Envelope struct {
	ReturnCode    int    `json:"returnCode"`
	ReturnMessage string `json:"returnMessage"`
}

// VersionResponse is the answer of POST /version.
type VersionResponse struct {
	Envelope
	Version        string `json:"version"`
	Database       string `json:"database"`
	Persistent     *bool  `json:"persistent"`
	StorageCeiling int64  `json:"storageCeiling"`
}

// Client talks to one stone. BaseURL is the API root, e.g.
// https://eds.example.com/api/v1.
type Client struct {
	BaseURL string
	HTTP    *http.Client // JSON operations
	Content *http.Client // uploads and downloads
}

// New makes a client for a stone's API root.
func New(baseURL string) *Client {
	return &Client{
		BaseURL: strings.TrimRight(baseURL, "/"),
		HTTP:    &http.Client{Timeout: 60 * time.Second},
		// Content may take long: no limit on the whole transfer, only the
		// caller's context.
		Content: &http.Client{},
	}
}

// call sends one JSON request and decodes the answer. The JSON API always
// answers HTTP 200 with the outcome in returnCode; the ID-based API answers
// with the return code as HTTP status. Both are handled.
func (c *Client) call(ctx context.Context, op Operation, token string, in, out any, env *Envelope) error {
	return c.callAt(ctx, op, op.Path, token, in, out, env)
}

// callAt is call with the operation's path filled in (IDs, a query). A nil
// request sends no body.
func (c *Client) callAt(ctx context.Context, op Operation, path, token string, in, out any, env *Envelope) error {
	var reader io.Reader
	if in != nil {
		body, err := json.Marshal(in)
		if err != nil {
			return exit.New(exit.Other, "%v", err)
		}
		reader = bytes.NewReader(body)
	}
	req, err := http.NewRequestWithContext(ctx, op.Method, c.BaseURL+path, reader)
	if err != nil {
		return exit.New(exit.Usage, "invalid stone URL %q: %v", c.BaseURL, err)
	}
	if in != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	req.Header.Set("Accept", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	resp, err := c.HTTP.Do(req)
	if err != nil {
		return &exit.Error{Code: exit.Network, Message: fmt.Sprintf("cannot reach %s: %v", c.BaseURL, rootCause(err))}
	}
	defer resp.Body.Close()
	data, err := io.ReadAll(io.LimitReader(resp.Body, 16<<20))
	if err != nil {
		return &exit.Error{Code: exit.Network, Message: fmt.Sprintf("reading the answer of %s: %v", c.BaseURL, err)}
	}
	if err := json.Unmarshal(data, out); err != nil || env.ReturnCode == 0 {
		return notEDS(op.Method, c.BaseURL+path, resp.StatusCode)
	}
	return env.err()
}

// notEDS is the error for an answer that isn't EDS's: a proxy's page, a
// wrong URL.
func notEDS(method, url string, status int) error {
	code := exit.Other
	if status == 502 || status == 503 || status == 504 {
		code = exit.Network
	}
	if status == 404 {
		code = exit.NotFound
	}
	return &exit.Error{Code: code, Message: fmt.Sprintf("%s %s: HTTP %d, not an EDS answer", method, url, status)}
}

// err turns a stone's return code into an error, or nil for success.
func (e *Envelope) err() error {
	if e.ReturnCode == 200 {
		return nil
	}
	return &exit.Error{
		Code:          exit.FromReturnCode(e.ReturnCode),
		Message:       e.ReturnMessage,
		ReturnCode:    e.ReturnCode,
		ReturnMessage: e.ReturnMessage,
	}
}

func rootCause(err error) error {
	for {
		next := errors.Unwrap(err)
		if next == nil {
			return err
		}
		err = next
	}
}
