package service

import (
	"context"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
)

// The status of stones (STATUS.md): what a stone tells about itself to
// those who look after it. A stone's entry in the container may keep a
// status key, which reads the status and nothing else; without one, the
// status is asked for with the session of its account, which must be the
// System Administrator's.

// Infos returns the stones of the container, without logging in.
func (s *Service) Infos() ([]agent.StoneInfo, error) {
	if err := s.load(); err != nil {
		return nil, err
	}
	return s.infos, nil
}

// Status asks a stone for its status. since is the last sample of the load
// which the caller has (0 for all); history asks for the day and all kept
// inventories.
func (s *Service) Status(ctx context.Context, name string, since int64, history bool) (*client.StatusResponse, error) {
	info, err := s.Info(name)
	if err != nil {
		return nil, err
	}
	url, _, key, err := s.access.Secrets(info.Name)
	if err != nil {
		return nil, err
	}
	if key != "" {
		return client.New(url).Status(ctx, key, since, history)
	}
	stone, err := s.Stone(ctx, info.Name)
	if err != nil {
		return nil, err
	}
	var status *client.StatusResponse
	err = stone.Do(ctx, func(token string) error {
		var err error
		status, err = stone.Client.Status(ctx, token, since, history)
		return err
	})
	return status, err
}
