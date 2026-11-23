package client

import (
	"context"
	"encoding/base64"
	"net/url"
	"strconv"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// Operation is one operation of the API that the client uses. Request and
// Response name the contract's schemas ("" for none).
type Operation struct {
	Method   string
	Path     string
	Request  string
	Response string
}

// The operations this client uses; the contract test checks them all.
var (
	OpVersion = Operation{"POST", "/version", "", "VersionResponse"}
	OpLogin   = Operation{"POST", "/members/login", "ProcessMemberRequest", "ProcessMemberResponse"}
	OpLogout  = Operation{"POST", "/members/logout", "ProcessMemberRequest", "ProcessMemberResponse"}

	OpMyTrustees    = Operation{"POST", "/trustees/fetchTrustees", "FetchTrusteeRequest", "FetchTrusteeResponse"}
	OpCircles       = Operation{"GET", "/circles", "", "FetchCircleResponse"}
	OpCreateCircle  = Operation{"POST", "/circles", "ProcessCircleRequest", "ProcessCircleResponse"}
	OpUpdateCircle  = Operation{"PUT", "/circles/{circleId}", "ProcessCircleRequest", "ProcessCircleResponse"}
	OpTrustees      = Operation{"GET", "/circles/{circleId}/trustees", "", "FetchTrusteeResponse"}
	OpAddTrustee    = Operation{"POST", "/circles/{circleId}/trustees", "ProcessTrusteeRequest", "ProcessTrusteeResponse"}
	OpAlterTrustee  = Operation{"PUT", "/circles/{circleId}/trustees/{memberId}", "ProcessTrusteeRequest", "ProcessTrusteeResponse"}
	OpRemoveTrustee = Operation{"DELETE", "/circles/{circleId}/trustees/{memberId}", "", "ProcessTrusteeResponse"}

	OpMembers      = Operation{"GET", "/members", "", "FetchMemberResponse"}
	OpMember       = Operation{"GET", "/members/{memberId}", "", "FetchMemberResponse"}
	OpCreateMember = Operation{"POST", "/members", "ProcessMemberRequest", "ProcessMemberResponse"}
	OpDeleteMember = Operation{"DELETE", "/members/{memberId}", "", "ProcessMemberResponse"}
	OpAlterMember  = Operation{"PUT", "/members/{memberId}", "ProcessMemberRequest", "ProcessMemberResponse"}
	OpInvite       = Operation{"POST", "/members/inviteMember", "ProcessMemberRequest", "ProcessMemberResponse"}
	OpUpdateMember = Operation{"POST", "/members/updateMember", "ProcessMemberRequest", "ProcessMemberResponse"}

	OpListRoot   = Operation{"GET", "/data", "", "FetchDataResponse"}
	OpListFolder = Operation{"GET", "/data/{dataId}", "", "FetchDataResponse"}
	OpAddData    = Operation{"POST", "/data", "ProcessDataRequest", "ProcessDataResponse"}
	OpUpdateData = Operation{"PUT", "/data/{dataId}", "ProcessDataRequest", "ProcessDataResponse"}
	OpDeleteData = Operation{"DELETE", "/data/{dataId}", "", "ProcessDataResponse"}
	OpMoveData   = Operation{"POST", "/data/{dataId}/move", "ProcessDataRequest", "ProcessDataResponse"}
	OpUpload     = Operation{"PUT", "/data/{dataId}/content", "", "ProcessDataResponse"}
	OpDownload   = Operation{"GET", "/data/{dataId}/content", "", ""}
	OpHead       = Operation{"HEAD", "/data/{dataId}/content", "", ""}

	OpSettings = Operation{"POST", "/settings", "SettingRequest", "SettingResponse"}
)

// Operations lists them for the contract test.
var Operations = []Operation{
	OpVersion, OpLogin, OpLogout,
	OpMyTrustees, OpCircles, OpCreateCircle, OpUpdateCircle, OpTrustees, OpAddTrustee, OpAlterTrustee, OpRemoveTrustee,
	OpMembers, OpMember, OpCreateMember, OpDeleteMember, OpAlterMember, OpInvite, OpUpdateMember,
	OpListRoot, OpListFolder, OpAddData, OpUpdateData, OpDeleteData, OpMoveData, OpUpload, OpDownload, OpHead,
	OpSettings,
	OpCircle, OpSetSecrets,
	OpFeatures, OpOfferFeature, OpWithdrawFeature, OpCircleFeatures, OpSwitchFeature, OpRemoveFeature,
	OpAddEntry, OpListEntries, OpGetEntry, OpChangeEntry, OpRemoveEntry, OpCloseEntry, OpCountEntries,
	OpStatus, OpStatusKeys, OpCreateStatusKey, OpWithdrawStatusKey,
}

// The credential types, trust levels and roles of the contract.
const (
	CredentialPassphrase = "PASSPHRASE"
	CredentialSession    = "SESSION"
	CredentialInvitation = "INVITATION"

	TrustRead  = "READ"
	TrustWrite = "WRITE"
	TrustAdmin = "ADMIN"
	TrustSysop = "SYSOP"

	RoleAdmin    = "ADMIN"
	RoleStandard = "STANDARD"

	TypeFolder = "folder"
)

// ProcessMemberRequest carries credentials and changes of an account.
type ProcessMemberRequest struct {
	AccountName    string  `json:"accountName,omitempty"`
	Credential     string  `json:"credential,omitempty"`
	CredentialType string  `json:"credentialType,omitempty"`
	NewAccountName string  `json:"newAccountName,omitempty"`
	NewCredential  string  `json:"newCredential,omitempty"`
	MemberRole     string  `json:"memberRole,omitempty"`
	Avatar         *string `json:"avatar,omitempty"`
	CircleID       string  `json:"circleId,omitempty"`
	TrustLevel     string  `json:"trustLevel,omitempty"`
	PublicKey      string  `json:"publicKey,omitempty"`
	Vault          string  `json:"vault,omitempty"`
	Secret         string  `json:"secret,omitempty"`
}

// ProcessMemberResponse is the answer to it.
type ProcessMemberResponse struct {
	Envelope
	MemberID   string `json:"memberId"`
	Session    string `json:"session"`
	Invitation string `json:"invitation"`
}

// Trustee is a membership: a user in a room, with an access level.
type Trustee struct {
	MemberID     string `json:"memberId"`
	AccountName  string `json:"accountName"`
	Avatar       string `json:"avatar"`
	CircleID     string `json:"circleId"`
	CircleName   string `json:"circleName"`
	CircleAvatar string `json:"circleAvatar"`
	TrustLevel   string `json:"trustLevel"`
	PublicKey    string `json:"publicKey"`
	Secret       string `json:"secret"`
	Added        string `json:"added"`
	Changed      string `json:"changed"`
}

// FetchTrusteeRequest asks for memberships with the session in the body.
type FetchTrusteeRequest struct {
	Credential     string `json:"credential"`
	CredentialType string `json:"credentialType"`
	CircleID       string `json:"circleId,omitempty"`
	MemberID       string `json:"memberId,omitempty"`
}

// FetchTrusteeResponse lists memberships.
type FetchTrusteeResponse struct {
	Envelope
	Trustees []Trustee `json:"trustees"`
}

// Circle is a room.
type Circle struct {
	CircleID   string `json:"circleId"`
	CircleName string `json:"circleName"`
	Avatar     string `json:"avatar"`
	CircleKey  string `json:"circleKey"`
	Added      string `json:"added"`
}

// FetchCircleResponse lists rooms.
type FetchCircleResponse struct {
	Envelope
	Circles []Circle `json:"circles"`
}

// ProcessCircleRequest creates or changes a room.
type ProcessCircleRequest struct {
	CircleName string  `json:"circleName,omitempty"`
	Avatar     *string `json:"avatar,omitempty"`
}

// ProcessCircleResponse is the answer to it.
type ProcessCircleResponse struct {
	Envelope
	CircleID string `json:"circleId"`
}

// ProcessTrusteeRequest adds or changes a membership.
type ProcessTrusteeRequest struct {
	MemberID   string `json:"memberId,omitempty"`
	TrustLevel string `json:"trustLevel,omitempty"`
	// Secret is the Trustee's secret; a pointer, as an empty one clears it.
	Secret    *string         `json:"secret,omitempty"`
	CircleKey *string         `json:"circleKey,omitempty"`
	Secrets   []TrusteeSecret `json:"secrets,omitempty"`
}

// ProcessTrusteeResponse is the answer to it.
type ProcessTrusteeResponse struct {
	Envelope
}

// Member is a user's account.
type Member struct {
	MemberID    string `json:"memberId"`
	AccountName string `json:"accountName"`
	MemberRole  string `json:"memberRole"`
	// The first System Administrator: it claimed the stone, and alone makes,
	// unmakes or deletes administrators.
	FirstAdministrator bool   `json:"firstAdministrator"`
	Avatar             string `json:"avatar"`
	Settings           string `json:"settings"`
	PublicKey          string `json:"publicKey"`
	Vault              string `json:"vault"`
	Added              string `json:"added"`
}

// FetchMemberResponse lists users, and for one user their rooms.
type FetchMemberResponse struct {
	Envelope
	Members []Member `json:"members"`
	Circles []Circle `json:"circles"`
}

// Metadata describes a file or folder.
type Metadata struct {
	DataID    string `json:"dataId"`
	CircleID  string `json:"circleId"`
	FolderID  string `json:"folderId"`
	DataName  string `json:"dataName"`
	TypeName  string `json:"typeName"`
	MediaType string `json:"mediaType"`
	Size      int64  `json:"size"`
	Added     string `json:"added"`
}

// IsFolder reports whether the entry is a folder.
func (m Metadata) IsFolder() bool { return m.TypeName == TypeFolder }

// FetchDataResponse is a page of a folder's entries.
type FetchDataResponse struct {
	Envelope
	Metadata []Metadata `json:"metadata"`
	Records  int64      `json:"records"`
}

// ProcessDataRequest registers, renames, moves or copies a file or folder.
type ProcessDataRequest struct {
	CircleID       string `json:"circleId,omitempty"`
	TargetCircleID string `json:"targetCircleId,omitempty"`
	FolderID       string `json:"folderId,omitempty"`
	TargetFolderID string `json:"targetFolderId,omitempty"`
	DataName       string `json:"dataName,omitempty"`
	TypeName       string `json:"typeName,omitempty"`
	MediaType      string `json:"mediaType,omitempty"`
}

// ProcessDataResponse is the answer to it, and to an upload.
type ProcessDataResponse struct {
	Envelope
	DataID string `json:"dataId"`
	Etag   string `json:"etag"`
}

// SettingRequest reads the stone's settings, after storing the changes.
type SettingRequest struct {
	Credential     string            `json:"credential"`
	CredentialType string            `json:"credentialType"`
	Settings       map[string]string `json:"settings,omitempty"`
}

// SettingResponse has all settings, and the choices of those with a fixed
// set of values.
type SettingResponse struct {
	Envelope
	Settings      map[string]string   `json:"settings"`
	AllowedValues map[string][]string `json:"allowedValues"`
}

// Types maps the contract's schema names to the Go types, for the contract
// test.
var Types = map[string]any{
	"VersionResponse":        VersionResponse{},
	"ProcessMemberRequest":   ProcessMemberRequest{},
	"ProcessMemberResponse":  ProcessMemberResponse{},
	"FetchTrusteeRequest":    FetchTrusteeRequest{},
	"FetchTrusteeResponse":   FetchTrusteeResponse{},
	"Trustee":                Trustee{},
	"FetchCircleResponse":    FetchCircleResponse{},
	"Circle":                 Circle{},
	"ProcessCircleRequest":   ProcessCircleRequest{},
	"ProcessCircleResponse":  ProcessCircleResponse{},
	"ProcessTrusteeRequest":  ProcessTrusteeRequest{},
	"ProcessTrusteeResponse": ProcessTrusteeResponse{},
	"FetchMemberResponse":    FetchMemberResponse{},
	"Member":                 Member{},
	"FetchDataResponse":      FetchDataResponse{},
	"Metadata":               Metadata{},
	"ProcessDataRequest":     ProcessDataRequest{},
	"ProcessDataResponse":    ProcessDataResponse{},
	"SettingRequest":         SettingRequest{},
	"SettingResponse":        SettingResponse{},
}

func b64(text string) string { return base64.StdEncoding.EncodeToString([]byte(text)) }

func id(value string) string { return url.PathEscape(value) }

// Version asks the stone for its version and kind of system; needs no login.
func (c *Client) Version(ctx context.Context) (*VersionResponse, error) {
	out := &VersionResponse{}
	return out, c.call(ctx, OpVersion, "", struct{}{}, out, &out.Envelope)
}

// Login starts a session with the account's passphrase and returns its token.
func (c *Client) Login(ctx context.Context, account, passphrase string) (string, error) {
	token, _, err := c.LoginAs(ctx, account, passphrase)
	return token, err
}

// LoginAs is Login, and also returns the ID of the account.
func (c *Client) LoginAs(ctx context.Context, account, passphrase string) (token, memberID string, err error) {
	in := ProcessMemberRequest{AccountName: account, Credential: b64(passphrase), CredentialType: CredentialPassphrase}
	out := &ProcessMemberResponse{}
	if err := c.call(ctx, OpLogin, "", in, out, &out.Envelope); err != nil {
		return "", "", err
	}
	if out.Session == "" {
		return "", "", exit.New(exit.Other, "the stone returned no session token")
	}
	return out.Session, out.MemberID, nil
}

// Logout ends the session with the given token.
func (c *Client) Logout(ctx context.Context, token string) error {
	in := ProcessMemberRequest{Credential: token, CredentialType: CredentialSession}
	out := &ProcessMemberResponse{}
	return c.call(ctx, OpLogout, "", in, out, &out.Envelope)
}

// MyTrustees lists the rooms the account is in, with its access to each. An
// account in no room gets an empty list.
func (c *Client) MyTrustees(ctx context.Context, token string) ([]Trustee, error) {
	out := &FetchTrusteeResponse{}
	err := c.call(ctx, OpMyTrustees, "", FetchTrusteeRequest{Credential: token, CredentialType: CredentialSession}, out, &out.Envelope)
	if out.ReturnCode == 404 || out.ReturnCode == 401 {
		return nil, nil
	}
	return out.Trustees, err
}

// Circles lists the rooms the account may see; the System Administrator
// sees all.
func (c *Client) Circles(ctx context.Context, token string) ([]Circle, error) {
	out := &FetchCircleResponse{}
	err := c.call(ctx, OpCircles, token, nil, out, &out.Envelope)
	return out.Circles, err
}

// CreateCircle makes a room; its creator administers it.
func (c *Client) CreateCircle(ctx context.Context, token, name string) (string, error) {
	out := &ProcessCircleResponse{}
	err := c.call(ctx, OpCreateCircle, token, ProcessCircleRequest{CircleName: name}, out, &out.Envelope)
	return out.CircleID, err
}

// UpdateCircle renames a room or sets its avatar ("" in avatar removes it).
func (c *Client) UpdateCircle(ctx context.Context, token, circleID string, change ProcessCircleRequest) error {
	out := &ProcessCircleResponse{}
	return c.callAt(ctx, OpUpdateCircle, "/circles/"+id(circleID), token, change, out, &out.Envelope)
}

// Trustees lists the members of a room.
func (c *Client) Trustees(ctx context.Context, token, circleID string) ([]Trustee, error) {
	out := &FetchTrusteeResponse{}
	err := c.callAt(ctx, OpTrustees, "/circles/"+id(circleID)+"/trustees", token, nil, out, &out.Envelope)
	return out.Trustees, err
}

// AddTrustee adds a user to a room.
func (c *Client) AddTrustee(ctx context.Context, token, circleID, memberID, trustLevel string) error {
	out := &ProcessTrusteeResponse{}
	in := ProcessTrusteeRequest{MemberID: memberID, TrustLevel: trustLevel}
	return c.callAt(ctx, OpAddTrustee, "/circles/"+id(circleID)+"/trustees", token, in, out, &out.Envelope)
}

// AlterTrustee changes a member's access to a room.
func (c *Client) AlterTrustee(ctx context.Context, token, circleID, memberID, trustLevel string) error {
	out := &ProcessTrusteeResponse{}
	in := ProcessTrusteeRequest{TrustLevel: trustLevel}
	return c.callAt(ctx, OpAlterTrustee, "/circles/"+id(circleID)+"/trustees/"+id(memberID), token, in, out, &out.Envelope)
}

// RemoveTrustee removes a member from a room.
func (c *Client) RemoveTrustee(ctx context.Context, token, circleID, memberID string) error {
	out := &ProcessTrusteeResponse{}
	return c.callAt(ctx, OpRemoveTrustee, "/circles/"+id(circleID)+"/trustees/"+id(memberID), token, nil, out, &out.Envelope)
}

// Members lists all users.
func (c *Client) Members(ctx context.Context, token string) ([]Member, error) {
	out := &FetchMemberResponse{}
	err := c.call(ctx, OpMembers, token, nil, out, &out.Envelope)
	return out.Members, err
}

// Member returns one user and the rooms they are in.
func (c *Client) Member(ctx context.Context, token, memberID string) (*FetchMemberResponse, error) {
	out := &FetchMemberResponse{}
	return out, c.callAt(ctx, OpMember, "/members/"+id(memberID), token, nil, out, &out.Envelope)
}

// CreateMember makes an account with a passphrase: role is RoleStandard
// for a user or RoleAdmin for an administrator. Only a System
// Administrator may, and only the first may make an administrator.
func (c *Client) CreateMember(ctx context.Context, token, name, passphrase, role string) (string, error) {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{NewAccountName: name, NewCredential: b64(passphrase), MemberRole: role}
	err := c.call(ctx, OpCreateMember, token, in, out, &out.Envelope)
	return out.MemberID, err
}

// SetRole makes a user an administrator (RoleAdmin) or an administrator a
// user (RoleStandard); only the first System Administrator may.
func (c *Client) SetRole(ctx context.Context, token, memberID, role string) error {
	out := &ProcessMemberResponse{}
	return c.callAt(ctx, OpAlterMember, "/members/"+id(memberID), token, ProcessMemberRequest{MemberRole: role}, out, &out.Envelope)
}

// DeleteMember deletes a user; their rooms and files stay.
func (c *Client) DeleteMember(ctx context.Context, token, memberID string) error {
	out := &ProcessMemberResponse{}
	return c.callAt(ctx, OpDeleteMember, "/members/"+id(memberID), token, nil, out, &out.Envelope)
}

// Invite invites a new user into a room and returns the invitation token.
// For a name that already has an account the token is empty: the user was
// added to the room.
func (c *Client) Invite(ctx context.Context, token, name, circleID, trustLevel string) (string, error) {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{Credential: token, CredentialType: CredentialSession, NewAccountName: name, CircleID: circleID, TrustLevel: trustLevel}
	err := c.call(ctx, OpInvite, "", in, out, &out.Envelope)
	return out.Invitation, err
}

// AcceptInvitation sets the passphrase of an invited account.
func (c *Client) AcceptInvitation(ctx context.Context, name, invitation, passphrase string) error {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{AccountName: name, Credential: invitation, CredentialType: CredentialInvitation, NewCredential: b64(passphrase)}
	return c.call(ctx, OpUpdateMember, "", in, out, &out.Envelope)
}

// ChangePassphrase replaces the passphrase of an account.
func (c *Client) ChangePassphrase(ctx context.Context, account, current, next string) error {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{AccountName: account, Credential: b64(current), CredentialType: CredentialPassphrase, NewCredential: b64(next)}
	return c.call(ctx, OpUpdateMember, "", in, out, &out.Envelope)
}

// SetAvatar sets the own avatar; "" removes it.
func (c *Client) SetAvatar(ctx context.Context, token, avatar string) error {
	out := &ProcessMemberResponse{}
	in := ProcessMemberRequest{Credential: token, CredentialType: CredentialSession, Avatar: &avatar}
	return c.call(ctx, OpUpdateMember, "", in, out, &out.Envelope)
}

// List returns all entries of a folder, or of a room's top level when
// folderID is empty, page by page. Hidden entries (the chat) are left out.
func (c *Client) List(ctx context.Context, token, circleID, folderID string) ([]Metadata, error) {
	var all []Metadata
	for page := 1; ; page++ {
		out := &FetchDataResponse{}
		query := "?pageSize=100&pageNumber=" + strconv.Itoa(page)
		var err error
		if folderID == "" {
			err = c.callAt(ctx, OpListRoot, "/data?circleId="+url.QueryEscape(circleID)+"&"+query[1:], token, nil, out, &out.Envelope)
		} else {
			err = c.callAt(ctx, OpListFolder, "/data/"+id(folderID)+query, token, nil, out, &out.Envelope)
		}
		if err != nil {
			return nil, err
		}
		all = append(all, out.Metadata...)
		if len(out.Metadata) == 0 || int64(len(all)) >= out.Records {
			return all, nil
		}
	}
}

// AddData registers a file (without content yet) or, with TypeFolder, makes
// a folder. An empty FolderID means the room's top level.
func (c *Client) AddData(ctx context.Context, token string, in ProcessDataRequest) (string, error) {
	out := &ProcessDataResponse{}
	err := c.call(ctx, OpAddData, token, in, out, &out.Envelope)
	return out.DataID, err
}

// UpdateData renames an entry (DataName) or moves it to a folder of its
// room (FolderID).
func (c *Client) UpdateData(ctx context.Context, token, dataID string, change ProcessDataRequest) error {
	out := &ProcessDataResponse{}
	return c.callAt(ctx, OpUpdateData, "/data/"+id(dataID), token, change, out, &out.Envelope)
}

// DeleteData deletes a file, or an empty folder.
func (c *Client) DeleteData(ctx context.Context, token, dataID string) error {
	out := &ProcessDataResponse{}
	return c.callAt(ctx, OpDeleteData, "/data/"+id(dataID), token, nil, out, &out.Envelope)
}

// MoveData moves a file to another room of the same stone.
func (c *Client) MoveData(ctx context.Context, token, dataID string, target ProcessDataRequest) (string, error) {
	out := &ProcessDataResponse{}
	err := c.callAt(ctx, OpMoveData, "/data/"+id(dataID)+"/move", token, target, out, &out.Envelope)
	return out.DataID, err
}

// Settings reads the stone's settings after storing the changes (all or
// none); only the System Administrator may.
func (c *Client) Settings(ctx context.Context, token string, changes map[string]string) (*SettingResponse, error) {
	out := &SettingResponse{}
	in := SettingRequest{Credential: token, CredentialType: CredentialSession, Settings: changes}
	return out, c.call(ctx, OpSettings, "", in, out, &out.Envelope)
}
