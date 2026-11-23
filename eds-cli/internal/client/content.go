package client

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// Upload sends the content of a registered file. With a length of -1 the
// content is sent without Content-Length (chunked), which the stone reads to
// its end: for pipes. It returns the ETag of the new content.
func (c *Client) Upload(ctx context.Context, token, dataID string, content io.Reader, length int64) (string, error) {
	target := c.BaseURL + "/data/" + id(dataID) + "/content"
	if length == 0 {
		content = http.NoBody
	}
	req, err := http.NewRequestWithContext(ctx, OpUpload.Method, target, content)
	if err != nil {
		return "", exit.New(exit.Usage, "invalid stone URL %q: %v", c.BaseURL, err)
	}
	// net/http sends chunked for a body of unknown length.
	req.ContentLength = length
	req.Header.Set("Content-Type", "application/octet-stream")
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)
	resp, err := c.Content.Do(req)
	if err != nil {
		if ctx.Err() != nil {
			return "", exit.New(exit.Other, "%v", ctx.Err())
		}
		return "", &exit.Error{Code: exit.Network, Message: fmt.Sprintf("the upload to %s failed: %v", c.BaseURL, rootCause(err))}
	}
	defer resp.Body.Close()
	out := &ProcessDataResponse{}
	data, _ := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if err := json.Unmarshal(data, out); err != nil || out.ReturnCode == 0 {
		return "", notEDS(OpUpload.Method, target, resp.StatusCode)
	}
	return out.Etag, out.err()
}

// Content is a download: the body, and what the headers say about it.
type Content struct {
	Body    io.ReadCloser
	Partial bool   // the stone sent the requested range (206), not all of it
	Length  int64  // bytes in Body; -1 if the stone didn't say
	Total   int64  // size of the whole content; -1 if the stone didn't say
	ETag    string // identifies this content, for resuming
	Type    string // media type
}

// Download fetches the content of a file. With from above zero it asks for
// the rest from that byte on, and with an ETag only if the content is still
// the same; otherwise the stone sends all of it (Partial is false).
func (c *Client) Download(ctx context.Context, token, dataID string, from int64, etag string) (*Content, error) {
	return c.content(ctx, OpDownload.Method, token, dataID, from, etag)
}

// Head asks for the size, ETag and type of a file's content, without it.
func (c *Client) Head(ctx context.Context, token, dataID string) (*Content, error) {
	content, err := c.content(ctx, OpHead.Method, token, dataID, 0, "")
	if content != nil {
		content.Body.Close()
	}
	return content, err
}

func (c *Client) content(ctx context.Context, method, token, dataID string, from int64, etag string) (*Content, error) {
	target := c.BaseURL + "/data/" + id(dataID) + "/content"
	req, err := http.NewRequestWithContext(ctx, method, target, nil)
	if err != nil {
		return nil, exit.New(exit.Usage, "invalid stone URL %q: %v", c.BaseURL, err)
	}
	req.Header.Set("Authorization", "Bearer "+token)
	if from > 0 {
		req.Header.Set("Range", "bytes="+strconv.FormatInt(from, 10)+"-")
		if etag != "" {
			req.Header.Set("If-Range", etag)
		}
	}
	resp, err := c.Content.Do(req)
	if err != nil {
		return nil, &exit.Error{Code: exit.Network, Message: fmt.Sprintf("cannot reach %s: %v", c.BaseURL, rootCause(err))}
	}
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusPartialContent {
		defer resp.Body.Close()
		env := &Envelope{}
		data, _ := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
		if err := json.Unmarshal(data, env); err != nil || env.ReturnCode == 0 {
			if method == http.MethodHead && resp.StatusCode >= 400 && resp.StatusCode < 600 {
				// HEAD has no body; the status is the return code.
				return nil, &exit.Error{Code: exit.FromReturnCode(resp.StatusCode), ReturnCode: resp.StatusCode,
					Message: fmt.Sprintf("%s %s: HTTP %d", method, target, resp.StatusCode)}
			}
			return nil, notEDS(method, target, resp.StatusCode)
		}
		return nil, env.err()
	}
	out := &Content{
		Body:    resp.Body,
		Partial: resp.StatusCode == http.StatusPartialContent,
		Length:  resp.ContentLength,
		Total:   resp.ContentLength,
		ETag:    resp.Header.Get("ETag"),
		Type:    resp.Header.Get("Content-Type"),
	}
	if out.Partial {
		// Content-Range: bytes from-to/total
		out.Total = -1
		if _, total, ok := strings.Cut(resp.Header.Get("Content-Range"), "/"); ok {
			if n, err := strconv.ParseInt(total, 10, 64); err == nil {
				out.Total = n
			}
		}
	}
	return out, nil
}
