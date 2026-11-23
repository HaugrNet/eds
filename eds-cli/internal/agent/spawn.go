package agent

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/config"
)

// Handover is what `tolk login` passes to the agent process on its standard
// input: the unlocked container, never written to disk.
type Handover struct {
	Container *config.Container `json:"container"`
	Lifetime  time.Duration     `json:"lifetime"`
}

// ChildArg is the hidden command that runs the agent.
const ChildArg = "__agent"

// Spawn starts the agent as a process of its own and waits until it answers.
func Spawn(container *config.Container, lifetime time.Duration) error {
	self, err := os.Executable()
	if err != nil {
		return err
	}
	cmd := exec.Command(self, ChildArg)
	stdin, err := cmd.StdinPipe()
	if err != nil {
		return err
	}
	// What the agent says if it can't start, e.g. that its socket can't be made.
	complaint, err := os.CreateTemp("", "tolk-agent-*.log")
	if err != nil {
		return err
	}
	defer os.Remove(complaint.Name())
	defer complaint.Close()
	cmd.Stderr = complaint
	detach(cmd)
	if err := cmd.Start(); err != nil {
		return err
	}
	if err := json.NewEncoder(stdin).Encode(Handover{Container: container, Lifetime: lifetime}); err != nil {
		return err
	}
	stdin.Close()
	_ = cmd.Process.Release()
	for deadline := time.Now().Add(5 * time.Second); time.Now().Before(deadline); time.Sleep(50 * time.Millisecond) {
		if Running() {
			return nil
		}
	}
	if said, _ := os.ReadFile(complaint.Name()); len(said) > 0 {
		return fmt.Errorf("%s", strings.TrimPrefix(strings.TrimSpace(string(said)), "tolk: "))
	}
	return fmt.Errorf("the agent did not start")
}

// RunChild is the agent process: it reads the handover and serves until it
// is stopped.
func RunChild() error {
	var handover Handover
	if err := json.NewDecoder(os.Stdin).Decode(&handover); err != nil || handover.Container == nil {
		return fmt.Errorf("the agent is started by 'tolk login'")
	}
	listener, err := Listen()
	if err != nil {
		return err
	}
	path, _ := SocketPath()
	defer os.Remove(path)
	return NewServer(handover.Container, handover.Lifetime).Serve(listener)
}
