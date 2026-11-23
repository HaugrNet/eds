package agent

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
)

// EnvDir overrides the directory of the agent's socket.
const EnvDir = "TOLK_AGENT_DIR"

// SocketPath returns the path of the agent's socket, in a directory only the
// user can open, and makes that directory.
func SocketPath() (string, error) {
	dir := os.Getenv(EnvDir)
	if dir == "" {
		switch {
		case os.Getenv("XDG_RUNTIME_DIR") != "":
			dir = filepath.Join(os.Getenv("XDG_RUNTIME_DIR"), "tolk")
		case runtime.GOOS == "windows":
			cache, err := os.UserCacheDir()
			if err != nil {
				return "", err
			}
			dir = filepath.Join(cache, "tolk", "agent")
		default:
			dir = filepath.Join(os.TempDir(), fmt.Sprintf("tolk-%d", os.Getuid()))
		}
	}
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return "", err
	}
	if runtime.GOOS != "windows" {
		info, err := os.Lstat(dir)
		if err != nil {
			return "", err
		}
		// A directory someone else made, or others can enter, isn't trusted.
		if !info.IsDir() || info.Mode().Perm()&0o077 != 0 {
			return "", fmt.Errorf("%s must be a directory only you can open (mode 0700)", dir)
		}
		if !ownedByUser(info) {
			return "", fmt.Errorf("%s is not owned by you", dir)
		}
	}
	path := filepath.Join(dir, "agent.sock")
	// A socket's path is limited by the system (about 100 bytes).
	if len(path) > 100 {
		return "", fmt.Errorf("the path of the agent's socket is too long (%d bytes): %s; set %s to a shorter directory", len(path), path, EnvDir)
	}
	return path, nil
}
