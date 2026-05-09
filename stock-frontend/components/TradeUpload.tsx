import React, { useState, useCallback } from 'react';
import { UploadCloud, FileSpreadsheet, X } from 'lucide-react';
import { useDropzone } from 'react-dropzone';

const TradeUpload: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<any>(null);
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

  const handleUpload = async () => {
    if (!file) return;

    setLoading(true);
    setError(null);
    setResult(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('http://localhost:8080/api/trades/upload', {
        method: 'POST',
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || '上传失败');
      }

      setResult(data);
    } catch (err: any) {
      setError(err.message || '发生未知错误');
    } finally {
      setLoading(false);
    }
  };

  const removeFile = () => {
    setFile(null);
    setResult(null);
    setError(null);
  };

  return (
    <div className="p-8 bg-slate-900 rounded-xl border border-slate-800 max-w-2xl mx-auto my-10">
      <h2 className="text-xl font-bold text-white mb-4">上传交割单 Excel 文件</h2>
      <p className="text-slate-400 mb-6">上传券商导出的交割单 Excel 文件（.xls 或 .xlsx），系统将自动解析并存入数据库。</p>

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

      <div className="mt-6 text-center">
        <button
          onClick={handleUpload}
          disabled={!file || loading}
          className="w-full sm:w-auto px-8 py-3 bg-blue-600 text-white rounded-lg font-semibold shadow-lg shadow-blue-900/20
            hover:bg-blue-700 transition-all
            disabled:bg-slate-700 disabled:cursor-not-allowed disabled:shadow-none"
        >
          {loading ? '正在解析...' : '上传并解析'}
        </button>
      </div>

      {error && (
        <div className="mt-6 p-4 bg-rose-950/50 border border-rose-800 text-rose-300 rounded-lg">
          <strong>错误:</strong> {error}
        </div>
      )}

      {result && (
        <div className="mt-6 p-4 bg-emerald-950/50 border border-emerald-800 text-emerald-300 rounded-lg">
          <h3 className="font-bold mb-2">解析成功！</h3>
          <p>总共解析到 {result.totalParsed} 条记录。</p>
          <p>成功保存 {result.savedCount} 条新记录。</p>
          <p>{result.filteredCount} 条记录已过滤（委托编号为0）。</p>
          <p>{result.duplicateCount} 条记录已存在，已跳过。</p>
          <p>{result.newStockCount} 只新股票已添加。</p>
        </div>
      )}
    </div>
  );
};

export default TradeUpload;
