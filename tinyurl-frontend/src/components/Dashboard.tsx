import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { UrlItem, CreateUrlResponse } from '../types';
import { UrlForm } from './UrlForm';
import { UrlList } from './UrlList';

interface DashboardProps {
  userId: string;
  onLogout: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ userId, onLogout }) => {
  const [urls, setUrls] = useState<UrlItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchUrls = async () => {
    setLoading(true);
    setError('');
    try {
      const userUrls = await apiService.getUserUrls();
      setUrls(userUrls);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch URLs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUrls();
  }, []);

  const handleUrlCreated = (newUrl: CreateUrlResponse) => {
    // Add the new URL to the top of the list if it wasn't already there
    if (!newUrl.existed) {
      const urlItem: UrlItem = {
        code: newUrl.code,
        shortUrl: newUrl.shortUrl,
        url: newUrl.url,
        createdAt: newUrl.createdAt
      };
      setUrls(prev => [urlItem, ...prev]);
    }
  };

  const handleUrlDeleted = async (code: string) => {
    try {
      await apiService.deleteUrl(code);
      setUrls(prev => prev.filter(url => url.code !== code));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete URL');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-semibold text-gray-900">TinyURL</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-500">
                Logged in as: <span className="font-medium text-gray-900">{userId}</span>
              </span>
              <button
                onClick={onLogout}
                className="text-sm text-indigo-600 hover:text-indigo-500"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        {/* URL Creation Form */}
        <div className="mb-8">
          <UrlForm onUrlCreated={handleUrlCreated} />
        </div>

        {/* Error Display */}
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-md p-4">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        {/* URL List */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-medium text-gray-900">My URLs</h2>
            <button
              onClick={fetchUrls}
              disabled={loading}
              className="text-sm text-indigo-600 hover:text-indigo-500 disabled:opacity-50"
            >
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>

          <UrlList
            urls={urls}
            onDelete={handleUrlDeleted}
            loading={loading}
          />
        </div>
      </div>
    </div>
  );
};