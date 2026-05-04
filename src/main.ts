// API 配置
const API_BASE = 'http://localhost:8083/api/strategies';

// 类型定义
interface StockInfo {
  code: string;
  name: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  trendData: number[];
  tradeDate?: string;
}

interface AppState {
  stocks: string[];
  selectedStock: string | null;
  stockInfo: StockInfo | null;
  activeTab: 'list' | 'detail' | 'init';
  isLoading: boolean;
  initStatus: string;
  terminalLogs: string[];
}

const state: AppState = {
  stocks: [],
  selectedStock: null,
  stockInfo: null,
  activeTab: 'list',
  isLoading: false,
  initStatus: '',
  terminalLogs: []
};

// API 请求函数
async function fetchStockList(): Promise<string[]> {
  const res = await fetch(`${API_BASE}/stocks`);
  return res.json();
}

async function fetchStockDetail(code: string): Promise<StockInfo> {
  const res = await fetch(`${API_BASE}/stock/${code}`);
  return res.json();
}

async function initHistoricalData(): Promise<void> {
  const res = await fetch(`${API_BASE}/init-history`, { method: 'POST' });
  return res.json();
}

async function fetchMinuteData(): Promise<void> {
  const res = await fetch(`${API_BASE}/fetch-minute-data`, { method: 'POST' });
  return res.json();
}

// 日志函数
function addLog(message: string, type: 'info' | 'success' | 'warning' | 'error' = 'info') {
  const timestamp = new Date().toLocaleTimeString();
  state.terminalLogs.unshift(`[${timestamp}] [${type.toUpperCase()}] ${message}`);
  if (state.terminalLogs.length > 100) {
    state.terminalLogs.pop();
  }
  render();
}

// 渲染函数
function renderHeader(): string {
  return `
    <header class="bg-dark-card border-b border-dark-border px-6 py-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-gradient-to-br from-primary to-cyan-400 rounded-lg flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
            </svg>
          </div>
          <h1 class="text-xl font-bold text-white">策略中心</h1>
        </div>
        <div class="flex items-center gap-4">
          <button onclick="switchTab('list')" class="${state.activeTab === 'list' ? 'text-primary' : 'text-gray-400 hover:text-white'} transition-colors">
            股票列表
          </button>
          <button onclick="switchTab('detail')" class="${state.activeTab === 'detail' ? 'text-primary' : 'text-gray-400 hover:text-white'} transition-colors">
            股票详情
          </button>
          <button onclick="switchTab('init')" class="${state.activeTab === 'init' ? 'text-primary' : 'text-gray-400 hover:text-white'} transition-colors">
            数据管理
          </button>
        </div>
      </div>
    </header>
  `;
}

function renderStockList(): string {
  if (state.isLoading && state.stocks.length === 0) {
    return `
      <div class="flex items-center justify-center py-20">
        <div class="loading-spinner"></div>
        <span class="ml-3 text-gray-400">加载中...</span>
      </div>
    `;
  }

  return `
    <div class="p-6">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-white">沪深A股列表</h2>
        <span class="text-gray-400 text-sm">共 ${state.stocks.length} 只股票</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
        ${state.stocks.map(code => `
          <button 
            onclick="selectStock('${code}')"
            class="bg-dark-hover hover:bg-gray-600 hover:border-primary border border-dark-border rounded-lg px-3 py-2 text-center transition-all duration-200 ${state.selectedStock === code ? 'border-primary bg-dark-card' : ''}"
          >
            <div class="text-primary font-medium">${code}</div>
          </button>
        `).join('')}
      </div>
    </div>
  `;
}

function renderStockDetail(): string {
  if (!state.selectedStock) {
    return `
      <div class="flex items-center justify-center py-20">
        <div class="text-gray-400">请先从左侧选择一只股票</div>
      </div>
    `;
  }

  if (state.isLoading) {
    return `
      <div class="flex items-center justify-center py-20">
        <div class="loading-spinner"></div>
        <span class="ml-3 text-gray-400">加载中...</span>
      </div>
    `;
  }

  if (!state.stockInfo) {
    return `
      <div class="flex items-center justify-center py-20">
        <div class="text-gray-400">未找到股票信息</div>
      </div>
    `;
  }

  const info = state.stockInfo;
  const priceChange = info.openPrice > 0 ? ((info.highPrice - info.openPrice) / info.openPrice * 100).toFixed(2) : '0.00';
  const isUp = parseFloat(priceChange) >= 0;

  return `
    <div class="p-6 animate-fade-in">
      <!-- 股票头部信息 -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold text-white">${info.name}</h2>
          <div class="flex items-center gap-4 mt-2">
            <span class="text-gray-400">${info.code}</span>
            <span class="${isUp ? 'text-green-500' : 'text-red-500'} font-semibold">
              ${isUp ? '▲' : '▼'} ${Math.abs(parseFloat(priceChange))}%
            </span>
          </div>
        </div>
        <div class="text-right">
          <div class="text-3xl font-bold ${isUp ? 'text-green-500' : 'text-red-500'}">
            ${info.highPrice.toFixed(2)}
          </div>
          <div class="text-gray-400 text-sm">最新价</div>
        </div>
      </div>

      <!-- 价格数据卡片 -->
      <div class="grid grid-cols-3 gap-4 mb-6">
        <div class="card p-4">
          <div class="text-gray-400 text-sm mb-1">开盘价</div>
          <div class="text-xl font-semibold text-white">${info.openPrice.toFixed(2)}</div>
        </div>
        <div class="card p-4">
          <div class="text-gray-400 text-sm mb-1">最高价</div>
          <div class="text-xl font-semibold text-green-500">${info.highPrice.toFixed(2)}</div>
        </div>
        <div class="card p-4">
          <div class="text-gray-400 text-sm mb-1">最低价</div>
          <div class="text-xl font-semibold text-red-500">${info.lowPrice.toFixed(2)}</div>
        </div>
      </div>

      <!-- 分时图 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">5分钟分时数据</span>
          <span class="text-white text-sm opacity-80">共 ${info.trendData?.length || 0} 条</span>
        </div>
        <div class="p-4">
          ${renderTrendChart(info.trendData || [])}
        </div>
      </div>
    </div>
  `;
}

function renderTrendChart(data: number[]): string {
  if (data.length === 0) {
    return '<div class="text-gray-400 text-center py-8">暂无分时数据</div>';
  }

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  const width = 100;
  const height = 40;
  const points = data.map((val, i) => {
    const x = (i / (data.length - 1)) * width;
    const y = height - ((val - min) / range) * height;
    return `${x},${y}`;
  }).join(' ');

  const polylinePoints = data.map((val, i) => {
    const x = (i / (data.length - 1)) * 100;
    const y = 100 - ((val - min) / range) * 100;
    return `${x},${y}`;
  }).join(' ');

  return `
    <div class="relative">
      <svg viewBox="0 0 100 100" class="w-full h-48" preserveAspectRatio="none">
        <!-- 网格线 -->
        <line x1="0" y1="25" x2="100" y2="25" stroke="#2d3748" stroke-width="0.2" />
        <line x1="0" y1="50" x2="100" y2="50" stroke="#2d3748" stroke-width="0.2" />
        <line x1="0" y1="75" x2="100" y2="75" stroke="#2d3748" stroke-width="0.2" />
        
        <!-- 价格标签 -->
        <text x="2" y="20" fill="#9ca3af" font-size="4">${max.toFixed(2)}</text>
        <text x="2" y="50" fill="#9ca3af" font-size="4">${((max + min) / 2).toFixed(2)}</text>
        <text x="2" y="85" fill="#9ca3af" font-size="4">${min.toFixed(2)}</text>
        
        <!-- 趋势线 -->
        <defs>
          <linearGradient id="chartGradient" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" style="stop-color:#06b6d4;stop-opacity:0.3" />
            <stop offset="100%" style="stop-color:#06b6d4;stop-opacity:0" />
          </linearGradient>
        </defs>
        <polygon points="0,100 ${polylinePoints} 100,100" fill="url(#chartGradient)" />
        <polyline points="${polylinePoints}" fill="none" stroke="#06b6d4" stroke-width="0.5" vector-effect="non-scaling-stroke" />
        <circle cx="${polylinePoints.split(' ').pop()?.split(',')[0]}" cy="${polylinePoints.split(' ').pop()?.split(',')[1]}" r="1" fill="#06b6d4" />
      </svg>
      
      <!-- 数据列表 -->
      <div class="mt-4 grid grid-cols-8 gap-2 text-xs">
        ${data.slice(0, 48).map(val => `
          <div class="bg-dark-hover rounded px-2 py-1 text-center">
            <div class="text-gray-400">${val.toFixed(2)}</div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

function renderInitPanel(): string {
  return `
    <div class="p-6">
      <h2 class="text-lg font-semibold text-white mb-6">数据管理</h2>
      
      <!-- 操作卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div class="card">
          <div class="card-header">
            <span class="card-title">初始化历史数据</span>
          </div>
          <div class="p-4">
            <p class="text-gray-400 text-sm mb-4">
              从新浪财经API拉取近一年内A股所有股票的基本信息、日线数据和5分钟分时数据。
            </p>
            <div class="flex gap-3">
              <button onclick="handleInitHistory()" class="btn-primary flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
                开始初始化
              </button>
              <button onclick="handleFetchMinuteData()" class="btn-secondary flex items-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                </svg>
                刷新分时数据
              </button>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <span class="card-title">数据统计</span>
          </div>
          <div class="p-4">
            <div class="grid grid-cols-2 gap-4">
              <div class="text-center">
                <div class="text-2xl font-bold text-primary">${state.stocks.length}</div>
                <div class="text-gray-400 text-sm">股票总数</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-green-500">--</div>
                <div class="text-gray-400 text-sm">已初始化</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 终端日志 -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">操作日志</span>
          <button onclick="clearLogs()" class="text-white text-sm hover:text-gray-300">清空</button>
        </div>
        <div class="terminal h-64 overflow-y-auto">
          ${state.terminalLogs.length === 0 
            ? '<div class="terminal-line text-gray-500">等待操作...</div>'
            : state.terminalLogs.map(log => {
                const isError = log.includes('[ERROR]');
                const isSuccess = log.includes('[SUCCESS]');
                const isWarning = log.includes('[WARNING]');
                return `<div class="terminal-line ${isError ? 'terminal-error' : isSuccess ? 'terminal-success' : isWarning ? 'terminal-warning' : 'terminal-info'}">${log}</div>`;
              }).join('')
          }
        </div>
      </div>
    </div>
  `;
}

function render(): void {
  const app = document.getElementById('app');
  if (!app) return;

  let mainContent = '';
  
  if (state.activeTab === 'list') {
    mainContent = `
      <div class="flex flex-1 overflow-hidden">
        <!-- 左侧股票列表 -->
        <div class="w-80 bg-dark-card border-r border-dark-border overflow-y-auto">
          ${renderStockList()}
        </div>
        <!-- 右侧股票详情 -->
        <div class="flex-1 overflow-y-auto">
          ${renderStockDetail()}
        </div>
      </div>
    `;
  } else if (state.activeTab === 'detail') {
    mainContent = `
      <div class="flex flex-1 overflow-hidden">
        <div class="w-80 bg-dark-card border-r border-dark-border overflow-y-auto">
          ${renderStockList()}
        </div>
        <div class="flex-1 overflow-y-auto">
          ${renderStockDetail()}
        </div>
      </div>
    `;
  } else {
    mainContent = `
      <div class="flex-1 overflow-y-auto">
        ${renderInitPanel()}
      </div>
    `;
  }

  app.innerHTML = `
    <div class="min-h-screen flex flex-col">
      ${renderHeader()}
      <main class="flex-1 overflow-hidden flex">
        ${mainContent}
      </main>
    </div>
  `;
}

// 事件处理函数
async function switchTab(tab: 'list' | 'detail' | 'init'): Promise<void> {
  state.activeTab = tab;
  render();
  
  if ((tab === 'list' || tab === 'detail') && state.stocks.length === 0) {
    await loadStockList();
  }
}

async function loadStockList(): Promise<void> {
  state.isLoading = true;
  render();
  
  try {
    addLog('正在获取股票列表...', 'info');
    state.stocks = await fetchStockList();
    addLog(`成功获取 ${state.stocks.length} 只股票`, 'success');
  } catch (error) {
    addLog(`获取股票列表失败: ${error}`, 'error');
  }
  
  state.isLoading = false;
  render();
}

async function selectStock(code: string): Promise<void> {
  state.selectedStock = code;
  state.isLoading = true;
  render();
  
  try {
    addLog(`正在获取 ${code} 详情...`, 'info');
    state.stockInfo = await fetchStockDetail(code);
    if (state.stockInfo) {
      addLog(`成功获取 ${state.stockInfo.name} (${code}) 详情`, 'success');
    } else {
      addLog(`未找到股票 ${code} 的信息`, 'warning');
    }
  } catch (error) {
    addLog(`获取股票详情失败: ${error}`, 'error');
  }
  
  state.isLoading = false;
  render();
}

async function handleInitHistory(): Promise<void> {
  if (!confirm('确认开始初始化历史数据？这将拉取近一年内所有A股数据，可能需要较长时间。')) {
    return;
  }
  
  addLog('开始初始化历史数据...', 'info');
  addLog('此任务在后台运行，请查看控制台了解进度', 'warning');
  
  try {
    await initHistoricalData();
    addLog('初始化任务已启动', 'success');
    addLog('请在控制台查看详细日志', 'info');
  } catch (error) {
    addLog(`初始化失败: ${error}`, 'error');
  }
}

async function handleFetchMinuteData(): Promise<void> {
  addLog('正在刷新分时数据...', 'info');
  
  try {
    await fetchMinuteData();
    addLog('分时数据刷新任务已启动', 'success');
  } catch (error) {
    addLog(`刷新失败: ${error}`, 'error');
  }
}

function clearLogs(): void {
  state.terminalLogs = [];
  render();
}

// 初始化
async function init(): Promise<void> {
  render();
  await loadStockList();
}

init();
