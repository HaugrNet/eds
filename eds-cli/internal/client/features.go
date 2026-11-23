package client

import (
	"context"
	"net/url"
	"strconv"
)

// The operations for chambers (E2E.md), features (FEATURES.md) and the status of
// a stone (STATUS.md).
var (
	OpCircle     = Operation{"GET", "/circles/{circleId}", "", "FetchCircleResponse"}
	OpSetSecrets = Operation{"PUT", "/circles/{circleId}/secrets", "ProcessTrusteeRequest", "ProcessTrusteeResponse"}

	OpFeatures        = Operation{"GET", "/features", "", "FeatureResponse"}
	OpOfferFeature    = Operation{"PUT", "/features/{feature}", "FeatureRequest", "FeatureResponse"}
	OpWithdrawFeature = Operation{"DELETE", "/features/{feature}", "", "FeatureResponse"}
	OpCircleFeatures  = Operation{"GET", "/circles/{circleId}/features", "", "CircleFeatureResponse"}
	OpSwitchFeature   = Operation{"PUT", "/circles/{circleId}/features/{feature}", "CircleFeatureRequest", "CircleFeatureResponse"}
	OpRemoveFeature   = Operation{"DELETE", "/circles/{circleId}/features/{feature}", "", "CircleFeatureResponse"}

	OpAddEntry     = Operation{"POST", "/circles/{circleId}/features/{feature}/entries", "EntryRequest", "EntryResponse"}
	OpListEntries  = Operation{"GET", "/circles/{circleId}/features/{feature}/entries", "", "EntryResponse"}
	OpGetEntry     = Operation{"GET", "/circles/{circleId}/features/{feature}/entries/{entryId}", "", "EntryResponse"}
	OpChangeEntry  = Operation{"PUT", "/circles/{circleId}/features/{feature}/entries/{entryId}", "EntryRequest", "EntryResponse"}
	OpRemoveEntry  = Operation{"DELETE", "/circles/{circleId}/features/{feature}/entries/{entryId}", "", "EntryResponse"}
	OpCloseEntry   = Operation{"PUT", "/circles/{circleId}/features/{feature}/entries/{entryId}/closed", "EntryRequest", "EntryResponse"}
	OpCountEntries = Operation{"GET", "/circles/{circleId}/features/{feature}/count", "", "EntryResponse"}

	OpStatus            = Operation{"GET", "/status", "", "StatusResponse"}
	OpStatusKeys        = Operation{"GET", "/status/keys", "", "StatusKeyResponse"}
	OpCreateStatusKey   = Operation{"POST", "/status/keys", "StatusKeyRequest", "StatusKeyResponse"}
	OpWithdrawStatusKey = Operation{"DELETE", "/status/keys/{label}", "", "StatusKeyResponse"}
)

// The states of a feature in a room.
const (
	FeatureOn       = "ON"
	FeatureReadonly = "READONLY"
	FeatureOff      = "OFF"
)

// TrusteeSecret is the secret of one Member of a Circle.
type TrusteeSecret struct {
	MemberID string `json:"memberId"`
	Secret   string `json:"secret"`
}

// Feature is a feature which the stone offers.
type Feature struct {
	Name        string `json:"name"`
	Version     int    `json:"version"`
	Declaration string `json:"declaration,omitempty"`
}

// FeatureRequest offers a feature, by its declaration.
type FeatureRequest struct {
	Declaration string `json:"declaration"`
}

// FeatureResponse lists the features of the stone.
type FeatureResponse struct {
	Envelope
	Features []Feature `json:"features"`
}

// CircleFeature is a feature in a room, with its state.
type CircleFeature struct {
	Name    string `json:"name"`
	Version int    `json:"version"`
	State   string `json:"state"`
}

// CircleFeatureRequest switches a feature of a room.
type CircleFeatureRequest struct {
	State string `json:"state"`
}

// CircleFeatureResponse lists the features of a room.
type CircleFeatureResponse struct {
	Envelope
	Features []CircleFeature `json:"features"`
}

// Entry is an entry of a feature: what the stone notes, and the content, which
// is the clients' (Base64; in a chamber it is encrypted).
type Entry struct {
	EntryID     string `json:"entryId"`
	CircleID    string `json:"circleId"`
	Feature     string `json:"feature,omitempty"`
	Kind        string `json:"kind"`
	Position    int64  `json:"position"`
	About       string `json:"about,omitempty"`
	Name        string `json:"name,omitempty"`
	MemberID    string `json:"memberId,omitempty"`
	AccountName string `json:"accountName,omitempty"`
	Added       string `json:"added,omitempty"`
	Changed     string `json:"changed,omitempty"`
	Closed      bool   `json:"closed,omitempty"`
	Content     string `json:"content,omitempty"`
}

// EntryRequest adds or changes an entry.
type EntryRequest struct {
	Kind    string  `json:"kind,omitempty"`
	About   string  `json:"about,omitempty"`
	Name    *string `json:"name,omitempty"`
	Content *string `json:"content,omitempty"`
	Closed  *bool   `json:"closed,omitempty"`
}

// EntryResponse carries entries, or a count.
type EntryResponse struct {
	Envelope
	Entries []Entry `json:"entries"`
	Count   int64   `json:"count"`
}

// EntryQuery says which entries of a feature to list.
type EntryQuery struct {
	Kind        string
	About       string
	Name        string // a prefix of the names
	After       int64
	Before      int64
	OldestFirst bool
	Limit       int
	// Bare leaves the contents out.
	Bare bool
}

// StatusSample is one sample of a series of the status.
type StatusSample struct {
	Sequence int64              `json:"sequence"`
	Time     string             `json:"time"`
	Values   map[string]float64 `json:"values"`
}

// StatusSeries is a series of samples, the oldest first.
type StatusSeries struct {
	Interval int            `json:"interval"`
	Samples  []StatusSample `json:"samples"`
}

// StatusResponse is what a stone tells about itself.
type StatusResponse struct {
	Envelope
	Name         string        `json:"name"`
	Version      string        `json:"version"`
	Awake        bool          `json:"awake"`
	Time         string        `json:"time"`
	Started      string        `json:"started"`
	Database     string        `json:"database"`
	Persistent   *bool         `json:"persistent"`
	DatabaseMark string        `json:"databaseMark"`
	Load         *StatusSeries `json:"load"`
	Day          *StatusSeries `json:"day"`
	Inventory    *StatusSeries `json:"inventory"`
}

// StatusKeyRequest names a new status key.
type StatusKeyRequest struct {
	Label string `json:"label"`
}

// StatusKeyResponse lists the labels of the status keys, or carries a new
// key.
type StatusKeyResponse struct {
	Envelope
	Keys []string `json:"keys"`
	Key  string   `json:"key"`
}

func init() {
	for name, value := range map[string]any{
		"TrusteeSecret": TrusteeSecret{}, "Feature": Feature{}, "FeatureRequest": FeatureRequest{}, "FeatureResponse": FeatureResponse{},
		"CircleFeature": CircleFeature{}, "CircleFeatureRequest": CircleFeatureRequest{}, "CircleFeatureResponse": CircleFeatureResponse{},
		"Entry": Entry{}, "EntryRequest": EntryRequest{}, "EntryResponse": EntryResponse{},
		"StatusSample": StatusSample{}, "StatusSeries": StatusSeries{}, "StatusResponse": StatusResponse{},
		"StatusKeyRequest": StatusKeyRequest{}, "StatusKeyResponse": StatusKeyResponse{},
	} {
		Types[name] = value
	}
}

// --- Chambers ------------------------------------------------------------------

// Circle returns one room, with its shared value.
func (c *Client) Circle(ctx context.Context, token, circleID string) (*Circle, error) {
	out := &FetchCircleResponse{}
	if err := c.callAt(ctx, OpCircle, "/circles/"+id(circleID), token, nil, out, &out.Envelope); err != nil {
		return nil, err
	}
	if len(out.Circles) == 0 {
		return &Circle{CircleID: circleID}, nil
	}
	return &out.Circles[0], nil
}

// OwnTrustee returns the account's own membership of a room, with its
// secret; nil if it isn't in the room.
func (c *Client) OwnTrustee(ctx context.Context, token, circleID, memberID string) (*Trustee, error) {
	out := &FetchTrusteeResponse{}
	in := FetchTrusteeRequest{Credential: token, CredentialType: CredentialSession, CircleID: circleID, MemberID: memberID}
	err := c.call(ctx, OpMyTrustees, "", in, out, &out.Envelope)
	if out.ReturnCode == 404 || out.ReturnCode == 401 || (err == nil && len(out.Trustees) == 0) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &out.Trustees[0], nil
}

// SetSecrets stores the secrets of Members of a room, and its shared value
// if one is given, in one step: all or none.
func (c *Client) SetSecrets(ctx context.Context, token, circleID string, secrets []TrusteeSecret, circleKey *string) error {
	if len(secrets) == 0 && circleKey == nil {
		return nil
	}
	out := &ProcessTrusteeResponse{}
	return c.callAt(ctx, OpSetSecrets, "/circles/"+id(circleID)+"/secrets", token, ProcessTrusteeRequest{Secrets: secrets, CircleKey: circleKey}, out, &out.Envelope)
}

// SetSecret stores one Member's secret of a room; every Member may set
// their own. An empty secret clears it.
func (c *Client) SetSecret(ctx context.Context, token, circleID, memberID, secret string) error {
	out := &ProcessTrusteeResponse{}
	return c.callAt(ctx, OpAlterTrustee, "/circles/"+id(circleID)+"/trustees/"+id(memberID), token, ProcessTrusteeRequest{Secret: &secret}, out, &out.Envelope)
}

// AddTrusteeWithSecret adds a user to a room, with their secret of it.
func (c *Client) AddTrusteeWithSecret(ctx context.Context, token, circleID, memberID, trustLevel, secret string) error {
	out := &ProcessTrusteeResponse{}
	in := ProcessTrusteeRequest{MemberID: memberID, TrustLevel: trustLevel}
	if secret != "" {
		in.Secret = &secret
	}
	return c.callAt(ctx, OpAddTrustee, "/circles/"+id(circleID)+"/trustees", token, in, out, &out.Envelope)
}

// InviteWithSecret invites a name into a room, with the secret which waits
// for them there. For a name that already has an account the invitation is
// empty and the Member's ID is returned: the user was added to the room.
func (c *Client) InviteWithSecret(ctx context.Context, token, name, circleID, trustLevel, secret string) (invitation, memberID string, err error) {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{Credential: token, CredentialType: CredentialSession, NewAccountName: name, CircleID: circleID, TrustLevel: trustLevel, Secret: secret}
	err = c.call(ctx, OpInvite, "", in, out, &out.Envelope)
	return out.Invitation, out.MemberID, err
}

// SetKeys stores the account's public key and its vault on the stone.
func (c *Client) SetKeys(ctx context.Context, token, publicKey, vault string) error {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{Credential: token, CredentialType: CredentialSession, PublicKey: publicKey, Vault: vault}
	return c.call(ctx, OpUpdateMember, "", in, out, &out.Envelope)
}

// --- Features ----------------------------------------------------------------------

// Features lists the features the stone offers.
func (c *Client) Features(ctx context.Context, token string) ([]Feature, error) {
	out := &FeatureResponse{}
	err := c.call(ctx, OpFeatures, token, nil, out, &out.Envelope)
	return out.Features, err
}

// OfferFeature lets the stone offer a feature, by its declaration; System
// Administrator only.
func (c *Client) OfferFeature(ctx context.Context, token, name, declaration string) error {
	out := &FeatureResponse{}
	return c.callAt(ctx, OpOfferFeature, "/features/"+id(name), token, FeatureRequest{Declaration: declaration}, out, &out.Envelope)
}

// WithdrawFeature stops the stone offering a feature; no room may have it.
func (c *Client) WithdrawFeature(ctx context.Context, token, name string) error {
	out := &FeatureResponse{}
	return c.callAt(ctx, OpWithdrawFeature, "/features/"+id(name), token, nil, out, &out.Envelope)
}

// CircleFeatures lists the features of a room with their state.
func (c *Client) CircleFeatures(ctx context.Context, token, circleID string) ([]CircleFeature, error) {
	out := &CircleFeatureResponse{}
	err := c.callAt(ctx, OpCircleFeatures, "/circles/"+id(circleID)+"/features", token, nil, out, &out.Envelope)
	return out.Features, err
}

// SwitchFeature switches a feature of a room on, off or to read-only.
func (c *Client) SwitchFeature(ctx context.Context, token, circleID, name, state string) error {
	out := &CircleFeatureResponse{}
	return c.callAt(ctx, OpSwitchFeature, "/circles/"+id(circleID)+"/features/"+id(name), token, CircleFeatureRequest{State: state}, out, &out.Envelope)
}

// RemoveFeature removes a feature from a room, with all its entries.
func (c *Client) RemoveFeature(ctx context.Context, token, circleID, name string) error {
	out := &CircleFeatureResponse{}
	return c.callAt(ctx, OpRemoveFeature, "/circles/"+id(circleID)+"/features/"+id(name), token, nil, out, &out.Envelope)
}

func entries(circleID, feature string) string {
	return "/circles/" + id(circleID) + "/features/" + id(feature) + "/entries"
}

func oneEntry(out *EntryResponse, err error) (*Entry, error) {
	if err != nil {
		return nil, err
	}
	if len(out.Entries) == 0 {
		return &Entry{}, nil
	}
	return &out.Entries[0], nil
}

// AddEntry adds an entry to a feature of a room.
func (c *Client) AddEntry(ctx context.Context, token, circleID, feature string, in EntryRequest) (*Entry, error) {
	out := &EntryResponse{}
	return oneEntry(out, c.callAt(ctx, OpAddEntry, entries(circleID, feature), token, in, out, &out.Envelope))
}

// ChangeEntry changes the content, the name, or both, of an entry.
func (c *Client) ChangeEntry(ctx context.Context, token, circleID, feature, entryID string, in EntryRequest) (*Entry, error) {
	out := &EntryResponse{}
	return oneEntry(out, c.callAt(ctx, OpChangeEntry, entries(circleID, feature)+"/"+id(entryID), token, in, out, &out.Envelope))
}

// GetEntry returns one entry.
func (c *Client) GetEntry(ctx context.Context, token, circleID, feature, entryID string) (*Entry, error) {
	out := &EntryResponse{}
	return oneEntry(out, c.callAt(ctx, OpGetEntry, entries(circleID, feature)+"/"+id(entryID), token, nil, out, &out.Envelope))
}

// RemoveEntry removes an entry; the answer has what went, the entries about
// it first.
func (c *Client) RemoveEntry(ctx context.Context, token, circleID, feature, entryID string) ([]Entry, error) {
	out := &EntryResponse{}
	err := c.callAt(ctx, OpRemoveEntry, entries(circleID, feature)+"/"+id(entryID), token, nil, out, &out.Envelope)
	return out.Entries, err
}

// CloseEntry closes an entry, or opens it again.
func (c *Client) CloseEntry(ctx context.Context, token, circleID, feature, entryID string, closed bool) (*Entry, error) {
	out := &EntryResponse{}
	return oneEntry(out, c.callAt(ctx, OpCloseEntry, entries(circleID, feature)+"/"+id(entryID)+"/closed", token, EntryRequest{Closed: &closed}, out, &out.Envelope))
}

// ListEntries returns a page of the entries of a feature, the newest first
// unless asked otherwise.
func (c *Client) ListEntries(ctx context.Context, token, circleID, feature string, query EntryQuery) ([]Entry, error) {
	values := url.Values{}
	set := func(name, value string) {
		if value != "" {
			values.Set(name, value)
		}
	}
	set("kind", query.Kind)
	set("about", query.About)
	set("name", query.Name)
	if query.After > 0 {
		values.Set("after", strconv.FormatInt(query.After, 10))
	}
	if query.Before > 0 {
		values.Set("before", strconv.FormatInt(query.Before, 10))
	}
	if query.OldestFirst {
		values.Set("oldestFirst", "true")
	}
	if query.Limit > 0 {
		values.Set("limit", strconv.Itoa(query.Limit))
	}
	if query.Bare {
		values.Set("content", "false")
	}
	path := entries(circleID, feature)
	if len(values) > 0 {
		path += "?" + values.Encode()
	}
	out := &EntryResponse{}
	err := c.callAt(ctx, OpListEntries, path, token, nil, out, &out.Envelope)
	return out.Entries, err
}

// AllEntries returns all entries of a kind, the oldest first, fetched in
// pages.
func (c *Client) AllEntries(ctx context.Context, token, circleID, feature string, query EntryQuery) ([]Entry, error) {
	const page = 200
	query.OldestFirst, query.Limit, query.Before = true, page, 0
	var all []Entry
	for {
		found, err := c.ListEntries(ctx, token, circleID, feature, query)
		if err != nil {
			return nil, err
		}
		all = append(all, found...)
		if len(found) < page {
			return all, nil
		}
		query.After = found[len(found)-1].Position
	}
}

// CountEntries counts the entries of a feature, of a kind and after a position
// if given.
func (c *Client) CountEntries(ctx context.Context, token, circleID, feature, kind string, after int64) (int64, error) {
	values := url.Values{}
	if kind != "" {
		values.Set("kind", kind)
	}
	if after > 0 {
		values.Set("after", strconv.FormatInt(after, 10))
	}
	path := "/circles/" + id(circleID) + "/features/" + id(feature) + "/count"
	if len(values) > 0 {
		path += "?" + values.Encode()
	}
	out := &EntryResponse{}
	err := c.callAt(ctx, OpCountEntries, path, token, nil, out, &out.Envelope)
	return out.Count, err
}

// --- Status ---------------------------------------------------------------------

// Status asks a stone for its status. The bearer is a status key, or the
// session of the System Administrator. since is the last sample of the load
// which the caller has (0 for all); history asks for the day and all kept
// inventories.
func (c *Client) Status(ctx context.Context, bearer string, since int64, history bool) (*StatusResponse, error) {
	values := url.Values{}
	if since > 0 {
		values.Set("since", strconv.FormatInt(since, 10))
	}
	if history {
		values.Set("history", "true")
	}
	path := "/status"
	if len(values) > 0 {
		path += "?" + values.Encode()
	}
	out := &StatusResponse{}
	err := c.callAt(ctx, OpStatus, path, bearer, nil, out, &out.Envelope)
	return out, err
}

// StatusKeys lists the labels of the status keys of the stone.
func (c *Client) StatusKeys(ctx context.Context, token string) ([]string, error) {
	out := &StatusKeyResponse{}
	err := c.call(ctx, OpStatusKeys, token, nil, out, &out.Envelope)
	return out.Keys, err
}

// CreateStatusKey makes a status key with a label; the key is returned
// once.
func (c *Client) CreateStatusKey(ctx context.Context, token, label string) (string, error) {
	out := &StatusKeyResponse{}
	err := c.call(ctx, OpCreateStatusKey, token, StatusKeyRequest{Label: label}, out, &out.Envelope)
	return out.Key, err
}

// WithdrawStatusKey withdraws the status key with the label.
func (c *Client) WithdrawStatusKey(ctx context.Context, token, label string) error {
	out := &StatusKeyResponse{}
	return c.callAt(ctx, OpWithdrawStatusKey, "/status/keys/"+id(label), token, nil, out, &out.Envelope)
}
