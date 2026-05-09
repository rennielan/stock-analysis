import React, { useState, useCallback } from 'react';
import { UploadCloud, FileSpreadsheet, X, CheckCircle } from 'lucide-react';
import { useDropzone } from 'react-dropzone';

interface HoldingUploadResult {
  message: string;
  successCount: number;
  failCount: number;
  notFoundCount: number;
  updatedStocks: Array<{
    id: string;
    code: string;
    name: string;
    referenceShares: number | null;
    costPrice: number | null;
  }>;
}

const HoldingUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<HoldingUploadResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles && acceptedFiles.length > 0) {
      setFile(acceptedFiles[0]);
      setResult(null);
      setError(null);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
    },
    multiple: false,
  });

  const removeFile = () => {
    setFile(null);
    setResult(null);
    setError(null);
  };

  const handleUpload = async () => {
    if (!file) return;

    setLoading(true);
    setError(null);
    setResult(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8080/api/trades/upload-holdings', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || '上传失败');
      }

      setResult(data);
    } catch (err: any) {
      setError(err.message || '上传过程中发生错误');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-white mb-2">持仓数据导入</h2>
        <p className="text-slate-400 text-sm">
          上传持仓 Excel 文件，批量更新股票的参考持股数量和成本价
        </p>
      </div>

      {/* 上传区域 */}
      <div
        {...getRootProps()}
        className={`relative p-10 border-2 border-dashed rounded-lg text-center cursor-pointer transition-colors
          ${isDragActive ? 'border-blue-500 bg-blue-950/30' : 'border-slate-700 hover:border-blue-600'}
        `}
      >
        <input {...getInputProps()} />
        <div className="flex flex-col items-center justify-center">
          <FileSpreadsheet className="w-12 h-12 text-slate-500 mb-4" />
          {isDragActive ? (
            <p className="text-blue-400">松开即可上传</p>
          ) : (
            <p className="text-slate-400">拖拽 Excel 文件到这里，或 <span className="text-blue-500 font-semibold">点击选择文件</span></p>
          )}
          <p className="text-xs text-slate-600 mt-2">支持 .xls 和 .xlsx 格式</p>
        </div>
      </div>

      {/* 文件信息 */}
      {file && (
        <div className="mt-6 p-4 bg-slate-800/50 rounded-lg flex items-center justify-between">
          <div className="flex items-center gap-3">
            <FileSpreadsheet className="w-6 h-6 text-blue-400" />
            <span className="text-sm text-slate-300">{file.name}</span>
            <span className="text-xs text-slate-500">({(file.size / 1024).toFixed(2)} KB)</span>
          </div>
          <button onClick={removeFile} className="p-1 text-slate-500 hover:text-rose-400">
            <X size={18} />
          </button>
        </div>
      )}

      {/* 上传按钮 */}
      <div className="mt-6 text-center">
        <button
          onClick={handleUpload}
          disabled={!file || loading}
          className="w-full sm:w-auto px-8 py-3 bg-blue-600 text-white rounded-lg font-semibold shadow-lg shadow-blue-900/20
            hover:bg-blue-700 transition-all
            disabled:bg-slate-700 disabled:cursor-not-allowed disabled:shadow-none"
        >
          {loading ? '正在解析...' : '上传并更新'}
        </button>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mt-6 p-4 bg-rose-950/50 border border-rose-800 text-rose-300 rounded-lg">
          <strong>错误:</strong> {error}
        </div>
      )}

      {/* 成功结果 */}
      {result && (
        <div className="mt-6 space-y-4">
          <div className="p-4 bg-emerald-950/50 border border-emerald-800 text-emerald-300 rounded-lg">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle className="w-5 h-5" />
              <h3 className="font-bold">导入成功！</h3>
            </div>
            <p className="text-sm">{result.message}</p>
            <div className="mt-3 grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-emerald-400 font-semibold">{result.successCount}</span>
                <span className="text-emerald-300/70 ml-2">条记录成功更新</span>
              </div>
              {result.failCount > 0 && (
                <div>
                  <span className="text-rose-400 font-semibold">{result.failCount}</span>
                  <span className="text-rose-300/70 ml-2">条记录失败</span>
                </div>
              )}
            </div>
          </div>

          {/* 更新的股票列表 */}
          {result.updatedStocks && result.updatedStocks.length > 0 && (
            <div className="bg-slate-900/50 border border-slate-800 rounded-lg overflow-hidden">
              <div className="px-4 py-3 bg-slate-800/50 border-b border-slate-800">
                <h4 className="text-sm font-semibold text-slate-300">更新的股票列表</h4>
              </div>
              <div className="max-h-96 overflow-y-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-800/30 text-slate-400">
                    <tr>
                      <th className="px-4 py-2 text-left font-medium">股票代码</th>
                      <th className="px-4 py-2 text-left font-medium">股票名称</th>
                      <th className="px-4 py-2 text-right font-medium">参考持股</th>
                      <th className="px-4 py-2 text-right font-medium">成本价</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {result.updatedStocks.map((stock, index) => (
                      <tr key={index} className="hover:bg-slate-800/30 transition-colors">
                        <td className="px-4 py-2 text-slate-300 font-mono">{stock.code}</td>
                        <td className="px-4 py-2 text-slate-300">{stock.name}</td>
                        <td className="px-4 py-2 text-right text-slate-300">
                          {stock.referenceShares !== null ? stock.referenceShares.toFixed(2) : '-'}
                        </td>
                        <td className="px-4 py-2 text-right text-slate-300">
                          {stock.costPrice !== null ? `¥${stock.costPrice.toFixed(4)}` : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 使用说明 */}
      <div className="mt-8 p-4 bg-slate-900/30 border border-slate-800 rounded-lg">
        <h4 className="text-sm font-semibold text-slate-300 mb-3">Excel 文件格式要求</h4>
        <div className="space-y-2 text-xs text-slate-400">
          <p><strong className="text-slate-300">必需列：</strong></p>
          <ul className="list-disc list-inside ml-2 space-y-1">
            <li><strong>证券代码</strong>（可能的列名：证券代码、股票代码、代码）- 支持 sh.600000 或 600000 格式</li>
          </ul>
          <p className="mt-2"><strong className="text-slate-300">可选列：</strong></p>
          <ul className="list-disc list-inside ml-2 space-y-1">
            <li><strong>参考持股</strong>（可能的列名：参考持股、持股数量、持仓数量、数量）</li>
            <li><strong>成本价</strong>（可能的列名：成本价、持仓成本、成本价格、买入成本）</li>
            <li><strong>证券名称</strong>（可能的列名：证券名称、股票名称、名称）</li>
          </ul>
          <p className="mt-2 text-slate-500">
            系统会根据证券代码与 stocks 表中的 symbol 字段进行匹配，找到则更新，不存在则创建新记录。
          </p>
        </div>
      </div>
    </div>
  );
};

export default HoldingUpload;
