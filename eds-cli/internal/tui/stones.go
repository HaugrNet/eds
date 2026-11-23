package tui

import (
	"context"
	"sort"
	"sync"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/status"
)

// The stones of the container as a System Administrator watches them
// (STATUS.md): a window over the panes with each stone's status, its bars
// and small curves, asked again every five seconds while it is open.

// stonesMsg brings what the stones told.
type stonesMsg struct{ views []status.View }

// openStones opens the window and asks the stones.
func (m *model) openStones() tea.Cmd {
	infos, err := m.opts.Service.Infos()
	if err != nil {
		return m.failed(err)
	}
	m.stones = nil
	for _, info := range infos {
		m.stones = append(m.stones, status.View{Name: info.Name, URL: info.URL})
	}
	m.over = &overlay{kind: overPage, title: i18n.Rune("Stones"), lines: []string{i18n.Rune("asking…")}, raw: true, stones: true}
	return m.askStones(true)
}

// askStones asks every stone of the container, at once; with history, also
// for the inventories it keeps.
func (m *model) askStones(history bool) tea.Cmd {
	if m.stonesBusy {
		return nil
	}
	m.stonesBusy, m.stonesAsked = true, time.Now()
	views := append([]status.View{}, m.stones...)
	svc, ctx := m.opts.Service, m.ctx
	return func() tea.Msg {
		asked, cancel := context.WithTimeout(ctx, 20*time.Second)
		defer cancel()
		var all sync.WaitGroup
		for i := range views {
			all.Add(1)
			go func() {
				defer all.Done()
				views[i].Ask(asked, svc.Status, history)
			}()
		}
		all.Wait()
		return stonesMsg{views: views}
	}
}

// showStones draws what the stones told into the window, the busiest first
// and one which doesn't answer before all.
func (m *model) showStones(msg stonesMsg) {
	m.stonesBusy = false
	m.stones = msg.views
	if m.over == nil || !m.over.stones {
		return
	}
	shown := append([]status.View{}, m.stones...)
	sort.SliceStable(shown, func(i, j int) bool {
		rank := func(w *status.View) float64 {
			if w.Problem != "" {
				return 2
			}
			return status.Busy(w.Load)
		}
		return rank(&shown[i]) > rank(&shown[j])
	})
	var lines []string
	for i := range shown {
		lines = append(lines, shown[i].Lines(m.locale)...)
	}
	m.over.lines = lines
}

// stonesTick asks the stones again while the window is open.
func (m *model) stonesTick(now time.Time) tea.Cmd {
	if m.over == nil || !m.over.stones || now.Sub(m.stonesAsked) < 5*time.Second {
		return nil
	}
	return m.askStones(false)
}
