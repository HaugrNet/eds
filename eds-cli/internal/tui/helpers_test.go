package tui

import "github.com/HaugrNet/eds/eds-cli/internal/client"

func newClient(url string) *client.Client { return client.New(url) }
