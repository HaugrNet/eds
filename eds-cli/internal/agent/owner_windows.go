//go:build windows

package agent

import (
	"os"
	"os/exec"
	"syscall"
)

func ownedByUser(os.FileInfo) bool { return true }

// detach makes the agent survive the console that started it.
func detach(cmd *exec.Cmd) {
	const detachedProcess = 0x00000008
	cmd.SysProcAttr = &syscall.SysProcAttr{CreationFlags: detachedProcess | syscall.CREATE_NEW_PROCESS_GROUP, HideWindow: true}
}
