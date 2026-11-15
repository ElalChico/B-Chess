/*
 * B-Chess V2 · Estadísticas con ECharts (carga perezosa desde CDN).
 * Gráficos: resumen (pie), flujo (sankey), aperturas (tree) y progreso (line).
 * Los datos provienen de las partidas guardadas en localStorage.
 */
window.BCHESS_STATS = (function () {
  'use strict';

  var EC_URL = 'https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js';
  var loading = null;
  var instances = {};
  var rendered = false;

  var C = {
    win: '#2ea043',
    loss: '#f85149',
    draw: '#d29922',
    white: '#58a6ff',
    black: '#a371f7',
    ai: '#f0883e',
    online: '#39d2c0',
    line1: '#58a6ff',
    line2: '#2ea043',
    line3: '#f0883e',
    text: '#e6e6e6',
    muted: '#8b949e',
    axis: '#21262d',
    border: '#30363d'
  };

  function loadEcharts() {
    if (window.echarts) return Promise.resolve();
    if (loading) return loading;
    loading = new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = EC_URL;
      s.onload = resolve;
      s.onerror = function () { loading = null; reject(new Error('No se pudo cargar ECharts')); };
      document.head.appendChild(s);
    });
    return loading;
  }

  function clearEmpty(el) {
    var note = el.querySelector('.chart-empty');
    if (note) note.remove();
  }

  function markEmpty(el, msg) {
    if (el.querySelector('.chart-empty')) return;
    var d = document.createElement('div');
    d.className = 'chart-empty';
    d.textContent = msg || 'Sin datos todavía: juega partidas para ver esta gráfica.';
    el.appendChild(d);
  }

  function dispose(id) {
    var chart = instances[id];
    if (chart) { chart.dispose(); delete instances[id]; }
  }

  function mount(id, option) {
    var el = document.getElementById(id);
    if (!el || !window.echarts) return;
    clearEmpty(el);
    var chart = instances[id] || echarts.init(el);
    instances[id] = chart;
    chart.setOption(option, true);
    chart.resize();
  }

  // ================= Resumen (pie) =================
  function optionSummary(games, stats) {
    var data = [];
    if (stats.wins) data.push({ value: stats.wins, name: 'Ganadas', itemStyle: { color: C.win } });
    if (stats.losses) data.push({ value: stats.losses, name: 'Perdidas', itemStyle: { color: C.loss } });
    if (stats.draws) data.push({ value: stats.draws, name: 'Empates', itemStyle: { color: C.draw } });
    var w = games.filter(function (g) { return g.localColor === 'w'; }).length;
    var b = games.length - w;
    if (w) data.push({ value: w, name: 'Blancas', itemStyle: { color: C.white } });
    if (b) data.push({ value: b, name: 'Negras', itemStyle: { color: C.black } });
    var ai = games.filter(function (g) { return g.mode === 'ai'; }).length;
    var on = games.length - ai;
    if (ai) data.push({ value: ai, name: 'vs IA', itemStyle: { color: C.ai } });
    if (on) data.push({ value: on, name: 'En línea', itemStyle: { color: C.online } });
    return {
      series: [{
        type: 'pie',
        radius: ['20%', '85%'],
        center: ['50%', '50%'],
        roseType: 'area',
        itemStyle: { borderRadius: 6, borderColor: '#0d1117', borderWidth: 3 },
        label: { show: true, color: C.text, fontSize: 12, formatter: '{b}\n{c}' },
        data: data
      }]
    };
  }

  // ================= Flujo (sankey) =================
  function firstMove(g) {
    var san = (g.movesSan && g.movesSan[0]) || extractSans(g.pgn)[0];
    if (!san) return null;
    var f = san.replace(/[+#]/g, '');
    if (f === 'e4' || f === 'd4' || f === 'c4' || f === 'Nf3' || f === 'f4' || f === 'g3' || f === 'b3') return f;
    return 'Otra';
  }

  function extractSans(pgn) {
    var sans = [];
    var text = String(pgn || '').replace(/\[[^\]]*\]/g, ' ');
    var re = /(?:\d+\.+\s*)?([NBRQKP][a-h1-8]?[x:@]?[a-h1-8]+|O-O(?:-O)?|[a-h][1-8](?:=[NBRQ])?)[+#]?\s*/g;
    var m;
    while ((m = re.exec(text))) sans.push(m[1]);
    return sans;
  }

  function reasonKind(g) {
    var r = String(g.reason || '').toLowerCase();
    if (r.indexOf('mate') >= 0) return 'Mate';
    if (r.indexOf('tiempo') >= 0) return 'Tiempo';
    if (r.indexOf('rendi') >= 0 || r.indexOf('aband') >= 0 || r.indexOf('desconec') >= 0) return 'Abandono';
    if (r.indexOf('tablas') >= 0) return 'Acuerdo';
    return 'Otro';
  }

  function optionFlow(games) {
    var nodes = [
      { name: 'Blancas', itemStyle: { color: C.white } },
      { name: 'Negras', itemStyle: { color: C.black } },
      { name: 'Ganadas', itemStyle: { color: C.win } },
      { name: 'Perdidas', itemStyle: { color: C.loss } },
      { name: 'Empates', itemStyle: { color: C.draw } },
      { name: 'Mate', itemStyle: { color: C.ai } },
      { name: 'Tiempo', itemStyle: { color: C.online } },
      { name: 'Abandono', itemStyle: { color: '#db61a2' } },
      { name: 'Otro', itemStyle: { color: C.muted } }
    ];
    var moves = {};
    games.forEach(function (g) {
      var mv = firstMove(g);
      if (!mv) return;
      if (!moves[mv]) moves[mv] = true;
      if (mv !== 'Otra' && !nodes.some(function (n) { return n.name === mv; })) {
        nodes.unshift({ name: mv, itemStyle: { color: mv === 'e4' ? C.white : (mv === 'd4' ? C.win : C.black) } });
      }
    });
    var counts = {};
    function bump(a, b) {
      var k = a + '→' + b;
      counts[k] = (counts[k] || 0) + 1;
    }
    games.forEach(function (g) {
      var mv = firstMove(g);
      var color = g.localColor === 'w' ? 'Blancas' : 'Negras';
      var res = g.myResult === 'win' ? 'Ganadas' : (g.myResult === 'loss' ? 'Perdidas' : 'Empates');
      if (mv) bump(mv, color);
      bump(color, res);
      bump(res, reasonKind(g));
    });
    var links = Object.keys(counts).map(function (k) {
      var parts = k.split('→');
      return { source: parts[0], target: parts[1], value: counts[k] };
    });
    return {
      series: [{
        type: 'sankey',
        layout: 'none',
        emphasis: { focus: 'adjacency' },
        nodeAlign: 'justify',
        nodeWidth: 20,
        nodeGap: 12,
        layoutIterations: 32,
        label: { color: C.text, fontSize: 11, position: 'right' },
        lineStyle: { color: 'gradient', curveness: 0.5, opacity: 0.4 },
        itemStyle: { borderWidth: 1, borderColor: C.border },
        data: nodes,
        links: links
      }]
    };
  }

  // ================= Aperturas (tree) =================
  function optionOpenings(games) {
    var root = {};
    games.forEach(function (g) {
      var sans = (g.movesSan && g.movesSan.length ? g.movesSan : extractSans(g.pgn)).slice(0, 6);
      if (!sans.length) return;
      var w1 = sans[0].replace(/[+#]/g, '');
      var b1 = sans[1] ? sans[1].replace(/[+#]/g, '') : '(sin respuesta)';
      var w2 = sans[2] ? sans[2].replace(/[+#]/g, '') : null;
      var b2 = sans[3] ? sans[3].replace(/[+#]/g, '') : null;
      root[w1] = root[w1] || {};
      root[w1][b1] = root[w1][b1] || { count: 0 };
      var branch = root[w1][b1];
      branch.count = (branch.count || 0) + 1;
      if (w2) branch[w2] = branch[w2] || {};
      if (b2) branch[w2][b2] = (branch[w2][b2] || 0) + 1;
    });
    var names = Object.keys(root);
    if (!names.length) return null;
    var data = names.map(function (w1) {
      var replies = root[w1];
      return {
        name: '1. ' + w1,
        children: Object.keys(replies).map(function (b1) {
          var leaf = replies[b1];
          var kids = Object.keys(leaf)
            .filter(function (k) { return k !== 'count'; })
            .sort(function (a, b) { return leaf[b] - leaf[a]; })
            .slice(0, 4)
            .map(function (w2) {
              return { name: '2. ' + w2, children: Object.keys(leaf[w2]).sort(function (a, b) { return leaf[w2][b] - leaf[w2][a]; }).slice(0, 3).map(function (b2) { return { name: '2… ' + b2 }; }) };
            });
          return { name: '1… ' + b1, children: kids };
        })
      };
    });
    return {
      series: [{
        type: 'tree',
        data: data,
        top: '8%',
        left: '8%',
        bottom: '8%',
        right: '20%',
        symbolSize: 10,
        orient: 'LR',
        label: { position: 'left', verticalAlign: 'middle', align: 'right', color: C.text, fontSize: 11 },
        leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
        expandAndCollapse: true,
        initialTreeDepth: 2,
        animationDuration: 550,
        animationDurationUpdate: 750,
        itemStyle: { color: C.white, borderColor: C.white },
        lineStyle: { color: C.border, width: 2, curveness: 0.5 }
      }]
    };
  }

  // ================= Progreso (line) =================
  function optionProgress(games) {
    if (!games.length) return null;
    var chrono = games.slice().sort(function (a, b) { return new Date(a.date) - new Date(b.date); });
    function seriesFor(filter, color) {
      var running = { n: 0, w: 0 };
      var pts = [];
      chrono.forEach(function (g) {
        if (!filter(g)) return;
        running.n++;
        if (g.myResult === 'win') running.w++;
        pts.push(running.n ? Math.round((running.w / running.n) * 1000) / 10 : null);
      });
      return pts;
    }
    var labels = chrono.map(function (g, i) { return 'P' + (i + 1); });
    function mk(name, color, filter) {
      return {
        name: name,
        type: 'line',
        smooth: 0.3,
        showSymbol: false,
        data: seriesFor(filter, color),
        lineStyle: { width: 4, color: color }
      };
    }
    return {
      tooltip: { trigger: 'axis' },
      legend: { textStyle: { color: C.muted }, top: 0 },
      grid: { left: '4%', right: '4%', bottom: '5%', top: '18%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: C.axis } },
        axisLabel: { color: C.muted, hideOverlap: true }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: C.axis, type: 'dashed' } },
        axisLabel: { color: C.muted, formatter: '{value}%' }
      },
      series: [
        mk('Todas', C.line1, function () { return true; }),
        mk('vs IA', C.line2, function (g) { return g.mode === 'ai'; }),
        mk('En línea', C.line3, function (g) { return g.mode === 'online'; })
      ]
    };
  }

  // ================= Resultados por color (line ×6) =================
  function optionColorForm(games) {
    if (!games.length) return null;
    var chrono = games.slice().sort(function (a, b) { return new Date(a.date) - new Date(b.date); });
    var labels = chrono.map(function (g, i) { return 'P' + (i + 1); });

    function seriesDef(name, color, dash, filter) {
      var acc = 0, pts = [];
      chrono.forEach(function (g) {
        if (filter(g)) acc++;
        pts.push(acc);
      });
      return {
        name: name,
        type: 'line',
        smooth: 0.25,
        showSymbol: false,
        step: false,
        data: pts,
        lineStyle: { width: 3, type: dash ? 'dashed' : 'solid', color: color },
        emphasis: { focus: 'series' }
      };
    }
    var isWhite = function (g) { return g.localColor === 'w'; };
    var isBlack = function (g) { return g.localColor !== 'w'; };
    var win = function (g) { return g.myResult === 'win'; };
    var loss = function (g) { return g.myResult === 'loss'; };
    var draw = function (g) { return g.myResult === 'draw'; };

    return {
      tooltip: { trigger: 'axis' },
      legend: { textStyle: { color: C.muted, fontSize: 11 }, top: 0, itemWidth: 18 },
      grid: { left: '4%', right: '4%', bottom: '5%', top: '24%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: C.axis } },
        axisLabel: { color: C.muted, hideOverlap: true }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: C.axis, type: 'dashed' } },
        axisLabel: { color: C.muted }
      },
      series: [
        seriesDef('Ganadas blancas', '#2ea043', false, function (g) { return isWhite(g) && win(g); }),
        seriesDef('Empates blancas', '#d29922', false, function (g) { return isWhite(g) && draw(g); }),
        seriesDef('Perdidas blancas', '#f85149', false, function (g) { return isWhite(g) && loss(g); }),
        seriesDef('Ganadas negras', '#3fb950', true, function (g) { return isBlack(g) && win(g); }),
        seriesDef('Empates negras', '#eac54f', true, function (g) { return isBlack(g) && draw(g); }),
        seriesDef('Perdidas negras', '#ff7b72', true, function (g) { return isBlack(g) && loss(g); })
      ]
    };
  }

  // ================= Render =================
  function render(games, profile) {
    loadEcharts().then(function () {
      var stats = window.BCHESS_PROFILE.computeStats(games);
      var summary = optionSummary(games, stats);
      var flow = optionFlow(games);
      var openings = optionOpenings(games);
      var progress = optionProgress(games);
      var colorForm = optionColorForm(games);
      rendered = true;

      var spec = [
        ['chart-summary', summary, 'Sin datos todavía'],
        ['chart-flow', games.length ? flow : null, 'Sin datos todavía'],
        ['chart-openings', openings, 'Sin aperturas registradas'],
        ['chart-progress', progress, 'Sin datos todavía'],
        ['chart-colorform', colorForm, 'Sin datos todavía']
      ];
      spec.forEach(function (s) {
        var id = s[0], opt = s[1], emptyMsg = s[2];
        var el = document.getElementById(id);
        if (!el) return;
        if (!opt) { dispose(id); markEmpty(el, emptyMsg); return; }
        mount(id, opt);
      });
    }).catch(function (err) {
      ['chart-summary', 'chart-flow', 'chart-openings', 'chart-progress', 'chart-colorform'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) markEmpty(el, 'No se pudo cargar ECharts (sin conexión a internet).');
      });
    });
  }

  function onProfileShown() {
    // Redimensiona los gráficos visibles cuando el panel se muestra.
    if (!rendered) return;
    setTimeout(function () {
      Object.keys(instances).forEach(function (id) {
        try { instances[id].resize(); } catch (e) {}
      });
    }, 60);
  }

  window.addEventListener('resize', function () {
    if (!rendered) return;
    Object.keys(instances).forEach(function (id) {
      try { instances[id].resize(); } catch (e) {}
    });
  });

  return { render: render, onProfileShown: onProfileShown };
})();
