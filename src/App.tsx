import { useState, useEffect } from 'react'

interface StockInfo {
  code: string
  name: string
  highPrice: number
  lowPrice: number
  openPrice: number
  trendData: number[]
}

function App() {
  const [stocks, setStocks] = useState<string[]>([])
  const [selectedStock, setSelectedStock] = useState<string>('')
  const [stockInfo, setStockInfo] = useState<StockInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [initLoading, setInitLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchStocks()
  }, [])

  const fetchStocks = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await fetch('/api/strategies/stocks')
      const data = await res.json()
      setStocks(data || [])
    } catch (e) {
      setError('获取股票列表失败，请检查后端服务')
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const fetchStockDetail = async (code: string) => {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`/api/strategies/stock/${code}`)
      const data = await res.json()
      setStockInfo(data)
    } catch (e) {
      setError('获取股票详情失败')
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const handleStockClick = (code: string) => {
    setSelectedStock(code)
    fetchStockDetail(code)
  }

  const handleInitData = async () => {
    setInitLoading(true)
    setError('')
    try {
      const res = await fetch('/api/strategies/init-history', { method: 'POST' })
      const data = await res.json()
      alert('数据初始化任务已启动，请查看后端日志了解进度')
      fetchStocks()
    } catch (e) {
      setError('启动初始化任务失败')
      console.error(e)
    } finally {
      setInitLoading(false)
    }
  }

  const renderTrendChart = (data: number[]) => {
    if (!data || data.length === 0) {
      return <div className="text-gray-500 text-center py-8">暂无分时数据</div>
    }

    const width = 600
    const height = 200
    const padding = 30
    const chartWidth = width - padding * 2
    const chartHeight = height - padding * 2

    const min = Math.min(...data)
    const max = Math.max(...data)
    const range = max - min || 1

    const points = data.map((value, index) => {
      const x = padding + (index / (data.length - 1)) * chartWidth
      const y = padding + chartHeight - ((value - min) / range) * chartHeight
      return `${x},${y}`
    }).join(' ')

    const pathD = data.map((value, index) => {
      const x = padding + (index / (data.length - 1)) * chartWidth
      const y = padding + chartHeight - ((value - min) / range) * chartHeight
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`
    }).join(' ')

    return (
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-48">
        {/* 网格线 */}
        <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="#374151" strokeWidth="1" />
        <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="#374151" strokeWidth="1" />
        {[0, 0.25, 0.5, 0.75, 1].map((ratio) => (
          <line
            key={ratio}
            x1={padding}
            y1={padding + chartHeight * (1 - ratio)}
            x2={width - padding}
            y2={padding + chartHeight * (1 - ratio)}
            stroke="#1f2937"
            strokeWidth="1"
            strokeDasharray="4"
          />
        ))}
        
        {/* 价格线 */}
        <polyline
          points={points}
          fill="none"
          stroke="#06b6d4"
          strokeWidth="2"
          vectorEffect="non-scaling-stroke"
        />
        
        {/* 面积填充 */}
        <path
          d={`${pathD} L ${width - padding} ${height - padding} L ${padding} ${height - padding} Z`}
          fill="url(#gradient)"
          opacity="0.3"
        />
        
        <defs>
          <linearGradient id="gradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#06b6d4" />
            <stop offset="100%" stopColor="#06b6d4" stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* 最高最低标签 */}
        <text x={padding} y={padding - 5} fill="#9ca3af" fontSize="10">最高: {max.toFixed(2)}</text>
        <text x={padding} y={height - padding + 15} fill="#9ca3af" fontSize="10">最低: {min.toFixed(2)}</text>
      </svg>
    )
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      {/* 顶部导航 */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h1 className="text-xl font-bold text-cyan-400">策略中心</h1>
            <span className="text-gray-500 text-sm">股票数据系统</span>
          </div>
          <div className="flex gap-3">
            <button
              onClick={fetchStocks}
              disabled={loading}
              className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded text-sm transition"
            >
              {loading ? '加载中...' : '刷新列表'}
            </button>
            <button
              onClick={handleInitData}
              disabled={initLoading}
              className="px-4 py-2 bg-cyan-600 hover:bg-cyan-500 rounded text-sm transition"
            >
              {initLoading ? '初始化中...' : '初始化数据'}
            </button>
          </div>
        </div>
      </header>

      <div className="flex h-[calc(100vh-73px)]">
        {/* 左侧股票列表 */}
        <aside className="w-80 bg-gray-800 border-r border-gray-700 flex flex-col">
          <div className="p-4 border-b border-gray-700">
            <h2 className="text-sm font-medium text-gray-400">
              股票列表 ({stocks.length} 只)
            </h2>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading && stocks.length === 0 ? (
              <div className="p-4 text-center text-gray-500">加载中...</div>
            ) : error ? (
              <div className="p-4 text-center text-red-400">{error}</div>
            ) : (
              <div className="grid grid-cols-3 gap-1 p-2">
                {stocks.map((code) => (
                  <button
                    key={code}
                    onClick={() => handleStockClick(code)}
                    className={`p-2 text-xs rounded transition ${
                      selectedStock === code
                        ? 'bg-cyan-600 text-white'
                        : 'bg-gray-700 hover:bg-gray-600 text-gray-300'
                    }`}
                  >
                    {code}
                  </button>
                ))}
              </div>
            )}
          </div>
        </aside>

        {/* 右侧详情 */}
        <main className="flex-1 p-6 overflow-y-auto">
          {stockInfo ? (
            <div className="space-y-6">
              {/* 股票信息卡片 */}
              <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <div className="flex items-start justify-between mb-6">
                  <div>
                    <h2 className="text-2xl font-bold text-white">{stockInfo.name}</h2>
                    <p className="text-gray-400 text-sm mt-1">{stockInfo.code}</p>
                  </div>
                  <div className="text-right">
                    <div className="text-3xl font-bold text-cyan-400">
                      {stockInfo.openPrice.toFixed(2)}
                    </div>
                    <p className="text-gray-400 text-sm mt-1">开盘价</p>
                  </div>
                </div>
                
                <div className="grid grid-cols-4 gap-4">
                  <div className="bg-gray-700 rounded-lg p-4">
                    <p className="text-gray-400 text-xs mb-1">最高价</p>
                    <p className="text-xl font-bold text-green-400">
                      {stockInfo.highPrice.toFixed(2)}
                    </p>
                  </div>
                  <div className="bg-gray-700 rounded-lg p-4">
                    <p className="text-gray-400 text-xs mb-1">最低价</p>
                    <p className="text-xl font-bold text-red-400">
                      {stockInfo.lowPrice.toFixed(2)}
                    </p>
                  </div>
                  <div className="bg-gray-700 rounded-lg p-4">
                    <p className="text-gray-400 text-xs mb-1">开盘价</p>
                    <p className="text-xl font-bold text-white">
                      {stockInfo.openPrice.toFixed(2)}
                    </p>
                  </div>
                  <div className="bg-gray-700 rounded-lg p-4">
                    <p className="text-gray-400 text-xs mb-1">涨跌幅</p>
                    <p className={`text-xl font-bold ${
                      stockInfo.highPrice >= stockInfo.openPrice ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {((stockInfo.highPrice - stockInfo.openPrice) / stockInfo.openPrice * 100).toFixed(2)}%
                    </p>
                  </div>
                </div>
              </div>

              {/* 分时图卡片 */}
              <div className="bg-gray-800 rounded-lg p-6 border border-gray-700">
                <h3 className="text-lg font-medium text-white mb-4">5分钟分时图</h3>
                {renderTrendChart(stockInfo.trendData)}
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <div className="text-gray-500 text-lg mb-2">请从左侧选择一个股票</div>
                <p className="text-gray-600 text-sm">
                  共 {stocks.length} 只股票可选
                </p>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

export default App
