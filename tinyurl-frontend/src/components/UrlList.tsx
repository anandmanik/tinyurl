import React from 'react';
import { UrlItem } from '../types';

interface UrlListProps {
  urls: UrlItem[];
  onDelete: (code: string) => void;
  loading: boolean;
}

export const UrlList: React.FC<UrlListProps> = ({ urls, onDelete, loading }) => {
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

  const openInNewTab = (url: string) => {
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  if (loading && urls.length === 0) {
    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex items-center justify-center h-32">
          <div className="text-sm text-gray-500">Loading your URLs...</div>
        </div>
      </div>
    );
  }

  if (urls.length === 0) {
    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="text-center py-8">
          <div className="text-gray-400 mb-2">
            <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
            </svg>
          </div>
          <h3 className="text-sm font-medium text-gray-900">No URLs yet</h3>
          <p className="text-sm text-gray-500 mt-1">
            Start by creating your first short URL above.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white shadow rounded-lg overflow-hidden">
      <div className="divide-y divide-gray-200">
        {urls.map((urlItem) => (
          <div key={urlItem.code} className="p-6 hover:bg-gray-50">
            <div className="flex items-start justify-between">
              <div className="flex-1 min-w-0">
                {/* Short URL */}
                <div className="mb-3">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                    Short URL
                  </label>
                  <div className="flex items-center mt-1">
                    <code className="flex-1 text-sm font-mono bg-gray-100 px-3 py-2 rounded text-indigo-600 break-all">
                      {urlItem.shortUrl}
                    </code>
                    <button
                      onClick={() => copyToClipboard(urlItem.shortUrl)}
                      className="ml-2 text-xs text-indigo-600 hover:text-indigo-500 px-3 py-2 border border-indigo-300 rounded hover:bg-indigo-50"
                      title="Copy to clipboard"
                    >
                      Copy
                    </button>
                  </div>
                </div>

                {/* Original URL */}
                <div className="mb-3">
                  <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                    Original URL
                  </label>
                  <p className="mt-1 text-sm text-gray-900 break-all">
                    {urlItem.url}
                  </p>
                </div>

                {/* Created Date */}
                <div className="text-xs text-gray-500">
                  Created: {new Date(urlItem.createdAt).toLocaleString()}
                </div>
              </div>

              {/* Actions */}
              <div className="ml-4 flex-shrink-0 flex space-x-2">
                <button
                  onClick={() => openInNewTab(urlItem.shortUrl)}
                  className="text-xs text-gray-600 hover:text-gray-500 px-3 py-2 border border-gray-300 rounded hover:bg-gray-50"
                  title="Open in new tab"
                >
                  Open
                </button>
                <button
                  onClick={() => onDelete(urlItem.code)}
                  className="text-xs text-red-600 hover:text-red-500 px-3 py-2 border border-red-300 rounded hover:bg-red-50"
                  title="Delete association"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};