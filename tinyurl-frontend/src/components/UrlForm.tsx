import React, { useState } from 'react';
import { apiService } from '../services/api';
import { CreateUrlResponse } from '../types';

interface UrlFormProps {
  onUrlCreated: (url: CreateUrlResponse) => void;
}

export const UrlForm: React.FC<UrlFormProps> = ({ onUrlCreated }) => {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<CreateUrlResponse | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!url.trim()) {
      setError('URL is required');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);

    try {
      let normalizedUrl = url.trim();

      // Auto-prefix https:// if no scheme is provided
      if (!normalizedUrl.startsWith('https://') && !normalizedUrl.startsWith('http://')) {
        normalizedUrl = 'https://' + normalizedUrl;
      }

      const response = await apiService.createUrl({ url: normalizedUrl });
      setResult(response);
      onUrlCreated(response);

      // Don't clear the form immediately to show the result
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create short URL');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      // Could add a toast notification here
    } catch (err) {
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  };

  const clearForm = () => {
    setUrl('');
    setResult(null);
    setError('');
  };

  return (
    <div className="bg-white shadow rounded-lg p-6">
      <h2 className="text-lg font-medium text-gray-900 mb-4">
        Create Short URL
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="url" className="block text-sm font-medium text-gray-700">
            URL to shorten
          </label>
          <div className="mt-1 flex rounded-md shadow-sm">
            <input
              type="url"
              id="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="flex-1 min-w-0 block w-full px-3 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              placeholder="example.com/your-long-url"
              disabled={loading}
            />
            <button
              type="submit"
              disabled={loading || !url.trim()}
              className="ml-3 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Shortening...' : 'Shorten'}
            </button>
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Will automatically prefix with https:// if no scheme is provided
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-3">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        {result && (
          <div className="bg-green-50 border border-green-200 rounded-md p-4">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <h4 className="text-sm font-medium text-green-800">
                  {result.existed ? 'URL Retrieved' : 'URL Created Successfully!'}
                </h4>
                <div className="mt-2 space-y-2">
                  <div>
                    <label className="text-xs font-medium text-green-700">Short URL:</label>
                    <div className="flex items-center mt-1">
                      <code className="flex-1 text-sm bg-white px-2 py-1 rounded border text-green-900">
                        {result.shortUrl}
                      </code>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(result.shortUrl)}
                        className="ml-2 text-xs text-green-600 hover:text-green-500 px-2 py-1 border border-green-300 rounded"
                      >
                        Copy
                      </button>
                    </div>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-green-700">Original URL:</label>
                    <p className="text-sm text-green-800 break-all">{result.url}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-green-700">Created:</label>
                    <p className="text-sm text-green-800">
                      {new Date(result.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={clearForm}
                className="ml-4 text-sm text-green-600 hover:text-green-500"
              >
                Clear
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
};