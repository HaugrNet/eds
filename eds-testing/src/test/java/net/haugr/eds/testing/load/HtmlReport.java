/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.testing.load;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * <p>The report of a load run as a single HTML page, next to the text
 * report: the load over time as charts, from the test's side (requests per
 * second and their latency, by kind) and from the stone's own samples (its
 * CPU, the load of the host, its heap, threads and socket connections),
 * then the table of operations and the text report.</p>
 *
 * <p>The page needs nothing else: the data is in it as JSON, and a few lines
 * of script draw the charts as SVG, with a crosshair and the values under
 * the pointer. It follows the light or dark setting of the system.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class HtmlReport {

    private HtmlReport() {
        // Only the static method.
    }

    /**
     * The page.
     *
     * @param settings  What the run was asked to do
     * @param metrics   What the test measured
     * @param samples   The stone's own samples, as GET /status gave them
     * @param loadStart When the load started, to place the samples in time
     * @param verdict   The outcome, e.g. "No failures."; and about the stone, if it was started
     * @param text      The text report, included as it is
     * @return HTML
     */
    // Every text written into the page goes through escape(), and the JSON of the charts has no "<" (it is
    // \u003c, the same to the script), so nothing can end its element; the taint analysis doesn't know that.
    @SuppressWarnings("findsecbugs:POTENTIAL_XML_INJECTION")
    static String render(final LoadSettings settings, final Metrics metrics, final List<JsonNode> samples, final Instant loadStart,
                         final List<String> verdict, final String text) {
        final long failed = metrics.failed();
        final StringBuilder page = new StringBuilder(65536);
        page.append(HEAD);
        page.append("<header><h1>EDS load test</h1><p class=\"sub\">").append(escape(settings.describe())).append("</p>");
        page.append("<p class=\"verdict ").append((failed == 0) ? "good\"><span aria-hidden=\"true\">✓</span> " : "bad\"><span aria-hidden=\"true\">✗</span> ");
        page.append(escape(verdict.getFirst())).append("</p>");
        verdict.stream().skip(1).forEach(line -> page.append("<p class=\"sub\">").append(escape(line)).append("</p>"));
        page.append("</header>\n");

        page.append("<section class=\"tiles\">");
        tile(page, "Requests", String.format(Locale.ROOT, "%,d", metrics.total()), String.format(Locale.ROOT, "%.0f per second", metrics.total() / metrics.seconds()));
        tile(page, "Failed", String.format(Locale.ROOT, "%,d", failed), (failed == 0) ? "none" : "see the examples below");
        for (final Metrics.Kind kind : Metrics.Kind.values()) {
            final double[] overall = metrics.overall(kind);
            tile(page, label(kind) + " p50", String.format(Locale.ROOT, "%.0f ms", overall[1]),
                    String.format(Locale.ROOT, "p90 %.0f ms, %,.0f measured", overall[2], overall[0]));
        }
        page.append("</section>\n");

        page.append("<h2 class=\"part\">The test's view</h2><section class=\"grid\">");
        chart(page, "throughput", "Requests per second", "Answered requests per second, by the way they went.");
        chart(page, "p50", "Latency, median (ms)", "Half of the requests took less; delivery: how long a chat message or signal took to reach the others.");
        chart(page, "p90", "Latency, 90th percentile (ms)", "Nine in ten took less.");
        page.append("</section>\n<h2 class=\"part\">The stone's view</h2><section class=\"grid\">");
        chart(page, "cpu", "CPU of the stone (%)", "Its share of all CPUs of the host.");
        chart(page, "load", "Load of the host", "The system load average, as the stone saw it.");
        chart(page, "heap", "Heap in use (MiB)", "The stone's used heap.");
        chart(page, "threads", "Threads of the stone", "Live platform threads; virtual threads are not counted.");
        chart(page, "sockets", "Socket connections and active users", "Open socket connections, and accounts active in the sample.");
        page.append("</section>\n");

        table(page, metrics);
        if (!metrics.samples().isEmpty()) {
            page.append("<h2 class=\"part\">Failed and expected outcomes</h2><ul class=\"samples\">");
            metrics.samples().forEach(sample -> page.append("<li>").append(escape(sample)).append("</li>"));
            page.append("</ul>\n");
        }
        page.append("<details><summary>The text report</summary><pre>").append(escape(text)).append("</pre></details>\n");

        page.append("<script>const DATA = ").append(data(metrics, samples, loadStart).replace("<", "\\u003c")).append(";</script>\n");
        page.append(SCRIPT).append("</body></html>\n");
        return page.toString();
    }

    private static String label(final Metrics.Kind kind) {
        return switch (kind) {
            case REST -> "REST";
            case SOCKET -> "Socket";
            case DELIVERY -> "Delivery";
        };
    }

    // Every text written into the page goes through escape() (also the numbers, which are
    // our own), so none can add markup; the taint analysis doesn't know escape().
    @SuppressWarnings("findsecbugs:POTENTIAL_XML_INJECTION")
    private static void tile(final StringBuilder page, final String label, final String value, final String detail) {
        page.append("<div class=\"tile\"><div class=\"label\">").append(escape(label)).append("</div><div class=\"value\">")
                .append(escape(value)).append("</div><div class=\"detail\">").append(escape(detail)).append("</div></div>");
    }

    private static void chart(final StringBuilder page, final String id, final String title, final String description) {
        page.append("<figure class=\"card\" id=\"").append(id).append("\"><figcaption><h3>").append(escape(title))
                .append("</h3><p>").append(escape(description)).append("</p></figcaption><div class=\"legend\"></div>")
                .append("<div class=\"plot\"><svg role=\"img\" aria-label=\"").append(escape(title)).append("\"></svg><div class=\"tip\" hidden></div></div></figure>");
    }

    // Every text written into the table goes through escape(); the taint analysis doesn't know it.
    @SuppressWarnings("findsecbugs:POTENTIAL_XML_INJECTION")
    private static void table(final StringBuilder page, final Metrics metrics) {
        page.append("<h2 class=\"part\">Operations</h2><div class=\"scroll\"><table><thead><tr><th>operation</th><th>count</th><th>per s</th>")
                .append("<th>expected</th><th>failed</th><th>p50 ms</th><th>p90 ms</th><th>p99 ms</th><th>max ms</th><th>mean ms</th>")
                .append("<th>sent MiB</th><th>recv MiB</th></tr></thead><tbody>");
        for (final Metrics.Row row : metrics.rows()) {
            page.append("<tr").append((row.failed() > 0) ? " class=\"failed\"" : "").append("><td>").append(escape(row.name())).append("</td>");
            cell(page, String.format(Locale.ROOT, "%,d", row.count()));
            cell(page, String.format(Locale.ROOT, "%.1f", row.perSecond()));
            cell(page, String.valueOf(row.expected()));
            cell(page, String.valueOf(row.failed()));
            for (final double value : new double[] { row.p50(), row.p90(), row.p99(), row.max(), row.mean() }) {
                cell(page, row.timed() ? String.format(Locale.ROOT, "%.1f", value) : "–");
            }
            cell(page, row.timed() ? String.format(Locale.ROOT, "%.2f", row.sentMiB()) : "–");
            cell(page, String.format(Locale.ROOT, "%.2f", row.receivedMiB()));
            page.append("</tr>");
        }
        page.append("</tbody></table></div>\n");
    }

    private static void cell(final StringBuilder page, final String value) {
        page.append("<td>").append(escape(value)).append("</td>");
    }

    /**
     * The data of the charts: the duration, the timeline of the test and the
     * stone's samples, each with the seconds since the load started.
     */
    private static String data(final Metrics metrics, final List<JsonNode> samples, final Instant loadStart) {
        final ObjectNode data = Http.JSON.createObjectNode();
        data.put("duration", metrics.loadSeconds());
        final ArrayNode timeline = data.putArray("timeline");
        for (final double[] step : metrics.timeline()) {
            final ArrayNode values = timeline.addArray();
            for (final double value : step) {
                values.add(value);
            }
        }
        final ArrayNode stone = data.putArray("stone");
        for (final JsonNode sample : samples) {
            final double at = secondsSince(loadStart, sample.path("time").asText());
            if (Double.isNaN(at) || (at < -Metrics.STEP_SECONDS) || (at > metrics.loadSeconds() + Metrics.STEP_SECONDS)) {
                continue;
            }
            final JsonNode values = sample.path("values");
            final ObjectNode point = stone.addObject();
            point.put("t", at);
            point.put("cpu", values.path("processShare").asDouble() * 100);
            point.put("load", values.path("systemLoad").asDouble());
            point.put("heap", values.path("memoryUsed").asDouble() / 1048576.0);
            point.put("threads", values.path("threads").asDouble());
            point.put("connections", values.path("connections").asDouble());
            point.put("accounts", values.path("accounts").asDouble());
        }
        try {
            return Http.JSON.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The stone's samples are in UTC, without a zone. */
    private static double secondsSince(final Instant start, final String time) {
        try {
            return (LocalDateTime.parse(time).toInstant(ZoneOffset.UTC).toEpochMilli() - start.toEpochMilli()) / 1000.0;
        } catch (DateTimeParseException _) {
            return Double.NaN;
        }
    }

    private static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String HEAD = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <title>EDS load test</title>
            <style>
            :root { color-scheme: light;
              --page: #f9f9f7; --surface: #fcfcfb; --ink: #0b0b0b; --ink-2: #52514e; --ink-3: #7a7974; --rule: #e4e3df; --grid: #ecebe7;
              --s1: #2a78d6; --s2: #eb6834; --s3: #1baf7a; --good: #006300; --bad: #d03b3b; }
            @media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { color-scheme: dark;
              --page: #0d0d0d; --surface: #1a1a19; --ink: #f0efec; --ink-2: #c3c2b7; --ink-3: #8f8e86; --rule: #33332f; --grid: #2a2a27;
              --s1: #3987e5; --s2: #d95926; --s3: #199e70; --good: #0ca30c; --bad: #e66767; } }
            :root[data-theme="dark"] { color-scheme: dark;
              --page: #0d0d0d; --surface: #1a1a19; --ink: #f0efec; --ink-2: #c3c2b7; --ink-3: #8f8e86; --rule: #33332f; --grid: #2a2a27;
              --s1: #3987e5; --s2: #d95926; --s3: #199e70; --good: #0ca30c; --bad: #e66767; }
            * { box-sizing: border-box; }
            body { margin: 0 auto; max-width: 1240px; padding: 24px 16px 48px; background: var(--page); color: var(--ink);
              font: 14px/1.45 system-ui, -apple-system, "Segoe UI", sans-serif; }
            h1 { font-size: 22px; margin: 0 0 4px; } h2.part { font-size: 16px; margin: 32px 0 12px; } h3 { font-size: 14px; margin: 0; }
            .sub { color: var(--ink-2); margin: 0 0 8px; } .verdict { margin: 0; font-weight: 600; }
            .verdict.good { color: var(--good); } .verdict.bad { color: var(--bad); }
            .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin-top: 20px; }
            .tile, .card { background: var(--surface); border: 1px solid var(--rule); border-radius: 10px; }
            .tile { padding: 12px 14px; } .tile .label { color: var(--ink-2); font-size: 12px; }
            .tile .value { font-size: 22px; font-weight: 600; font-variant-numeric: tabular-nums; } .tile .detail { color: var(--ink-3); font-size: 12px; }
            .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 520px), 1fr)); gap: 16px; }
            .card { margin: 0; padding: 14px 14px 8px; min-width: 0; } figcaption p { color: var(--ink-2); font-size: 12px; margin: 2px 0 6px; }
            .legend { display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; color: var(--ink-2); min-height: 18px; }
            .legend span::before { content: ""; display: inline-block; width: 14px; height: 2px; vertical-align: middle; margin-right: 6px; background: var(--c); }
            .plot { position: relative; } svg { display: block; width: 100%; height: auto; overflow: visible; }
            svg text { fill: var(--ink-3); font-size: 11px; font-variant-numeric: tabular-nums; } svg .name { fill: var(--ink-2); }
            svg .grid line { stroke: var(--grid); } svg .axis { stroke: var(--rule); }
            svg .line { fill: none; stroke-width: 2; stroke-linejoin: round; stroke-linecap: round; }
            svg .end { stroke: var(--surface); stroke-width: 2; } svg .cross { stroke: var(--ink-3); stroke-dasharray: 3 3; }
            .tip { position: absolute; top: 0; pointer-events: none; background: var(--surface); border: 1px solid var(--rule); border-radius: 6px;
              padding: 6px 8px; font-size: 12px; box-shadow: 0 2px 8px rgb(0 0 0 / 15%); white-space: nowrap; font-variant-numeric: tabular-nums; }
            .tip b { display: block; color: var(--ink-2); font-weight: 600; } .tip i { display: inline-block; width: 10px; height: 2px; margin-right: 6px; vertical-align: middle; }
            .scroll { overflow-x: auto; background: var(--surface); border: 1px solid var(--rule); border-radius: 10px; }
            table { border-collapse: collapse; width: 100%; font-size: 12px; font-variant-numeric: tabular-nums; }
            th, td { padding: 4px 10px; text-align: right; border-bottom: 1px solid var(--grid); white-space: nowrap; }
            th:first-child, td:first-child { text-align: left; } th { color: var(--ink-2); font-weight: 600; position: sticky; top: 0; background: var(--surface); }
            tr.failed td { color: var(--bad); font-weight: 600; } .samples { font-size: 12px; color: var(--ink-2); }
            details { margin-top: 24px; } summary { cursor: pointer; color: var(--ink-2); }
            pre { overflow-x: auto; font-size: 11px; background: var(--surface); border: 1px solid var(--rule); border-radius: 10px; padding: 12px; }
            </style></head><body>
            """;

    private static final String SCRIPT = """
            <script>
            (() => {
              const NS = 'http://www.w3.org/2000/svg', W = 640, H = 230, M = { l: 50, r: 76, t: 10, b: 28 };
              const el = (name, attrs, parent) => { const e = document.createElementNS(NS, name);
                for (const k in attrs) e.setAttribute(k, attrs[k]); if (parent) parent.appendChild(e); return e; };
              const nice = (raw) => { if (!(raw > 0)) return 1; const p = Math.pow(10, Math.floor(Math.log10(raw)));
                for (const m of [1, 2, 2.5, 5, 10]) if (m * p >= raw) return m * p; return 10 * p; };
              const fmt = (v) => v >= 100 ? Math.round(v).toLocaleString('en') : v >= 10 ? v.toFixed(1) : v.toFixed(2);
              const T = DATA.timeline, S = DATA.stone;
              const perSecond = (i) => T.map(r => [r[0], r[i] / r[11]]);
              const when = (count, value) => T.map(r => r[count] > 0 ? [r[0], r[value]] : null);
              const stone = (key) => S.map(p => [p.t, p[key]]);
              const charts = {
                throughput: [['REST', perSecond(1)], ['Socket', perSecond(4)]],
                p50: [['REST', when(1, 2)], ['Socket', when(4, 5)], ['Delivery', when(7, 8)]],
                p90: [['REST', when(1, 3)], ['Socket', when(4, 6)], ['Delivery', when(7, 9)]],
                cpu: [['CPU', stone('cpu')]], load: [['Load', stone('load')]], heap: [['Heap', stone('heap')]],
                threads: [['Threads', stone('threads')]],
                sockets: [['Connections', stone('connections')], ['Active users', stone('accounts')]]
              };
              for (const id in charts) draw(document.getElementById(id), charts[id]);

              function draw(card, series) {
                const svg = card.querySelector('svg'), tip = card.querySelector('.tip'), legend = card.querySelector('.legend');
                svg.setAttribute('viewBox', `0 0 ${W} ${H}`);
                const xMin = Math.min(0, ...series.flatMap(s => s[1].filter(Boolean).map(p => p[0])));
                const xMax = Math.max(DATA.duration, ...series.flatMap(s => s[1].filter(Boolean).map(p => p[0])));
                const top = 1.05 * Math.max(0, ...series.flatMap(s => s[1].filter(Boolean).map(p => p[1])));
                const yStep = nice(top / 4), yMax = Math.max(yStep, Math.ceil(top / yStep) * yStep);
                const x = v => M.l + (v - xMin) / (xMax - xMin || 1) * (W - M.l - M.r);
                const y = v => H - M.b - v / yMax * (H - M.t - M.b);
                const grid = el('g', { class: 'grid' }, svg);
                for (let v = 0; v <= yMax + yStep / 2; v += yStep) {
                  el('line', { x1: M.l, x2: W - M.r, y1: y(v), y2: y(v) }, grid);
                  el('text', { x: M.l - 6, y: y(v) + 4, 'text-anchor': 'end' }, svg).textContent =
                    Number.isInteger(yStep) ? v.toLocaleString('en') : String(+v.toFixed(2)); }
                el('line', { class: 'axis', x1: M.l, x2: W - M.r, y1: y(0), y2: y(0) }, svg);
                const xStep = nice((xMax - xMin) / 6);
                for (let v = Math.ceil(xMin / xStep) * xStep; v <= xMax; v += xStep)
                  el('text', { x: x(v), y: H - 8, 'text-anchor': 'middle' }, svg).textContent = v + ' s';
                if (series.length > 1) legend.innerHTML = series.map((s, i) => `<span style="--c: var(--s${i + 1})">${s[0]}</span>`).join('');
                const labels = [];
                series.forEach((s, i) => {
                  const color = `var(--s${i + 1})`;
                  let d = '', pen = 'M';
                  for (const p of s[1]) { if (!p) { pen = 'M'; continue; } d += `${pen}${x(p[0]).toFixed(1)},${y(p[1]).toFixed(1)}`; pen = 'L'; }
                  el('path', { class: 'line', d, style: `stroke: ${color}` }, svg);
                  const last = s[1].filter(Boolean).at(-1);
                  if (last) {
                    el('circle', { class: 'end', cx: x(last[0]), cy: y(last[1]), r: 4, style: `fill: ${color}` }, svg);
                    labels.push({ name: s[0], x: x(last[0]) + 8, y: y(last[1]) + 4 });
                  }
                });
                // Names at the ends of the lines, moved apart where the lines end close together.
                if (series.length > 1) {
                  labels.sort((a, b) => a.y - b.y);
                  for (let i = 1; i < labels.length; i++) labels[i].y = Math.max(labels[i].y, labels[i - 1].y + 13);
                  const over = labels.length ? labels.at(-1).y - (H - M.b) : 0;
                  if (over > 0) labels.forEach(l => l.y -= over);
                  for (const l of labels) el('text', { class: 'name', x: l.x, y: l.y }, svg).textContent = l.name;
                }
                const cross = el('line', { class: 'cross', y1: M.t, y2: H - M.b, visibility: 'hidden' }, svg);
                const xs = [...new Set(series.flatMap(s => s[1].filter(Boolean).map(p => p[0])))].sort((a, b) => a - b);
                const hit = el('rect', { x: M.l, y: M.t, width: W - M.l - M.r, height: H - M.t - M.b, fill: 'transparent' }, svg);
                hit.addEventListener('pointermove', e => {
                  if (!xs.length) return;
                  const box = svg.getBoundingClientRect(), at = xMin + ((e.clientX - box.left) / box.width * W - M.l) / (W - M.l - M.r) * (xMax - xMin);
                  const t = xs.reduce((a, b) => Math.abs(b - at) < Math.abs(a - at) ? b : a);
                  cross.setAttribute('x1', x(t)); cross.setAttribute('x2', x(t)); cross.setAttribute('visibility', 'visible');
                  tip.innerHTML = `<b>${Math.round(t)} s</b>` + series.map((s, i) => {
                    const p = s[1].filter(Boolean).find(q => q[0] === t);
                    return p ? `<div><i style="background: var(--s${i + 1})"></i>${s[0]}: ${fmt(p[1])}</div>` : '';
                  }).join('');
                  tip.hidden = false;
                  const left = x(t) / W * box.width;
                  tip.style.left = (left > box.width / 2 ? left - tip.offsetWidth - 12 : left + 12) + 'px';
                });
                hit.addEventListener('pointerleave', () => { tip.hidden = true; cross.setAttribute('visibility', 'hidden'); });
              }
            })();
            </script>
            """;
}
