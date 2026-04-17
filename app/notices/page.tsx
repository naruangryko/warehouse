'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Plus, Calendar, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Notice = {
  id: number;
  title: string;
  content: string;
  date: string;
  category: 'general' | 'event' | 'urgent';
};

export default function NoticesPage() {
  const [notices, setNotices] = useState<Notice[]>([]);

  useEffect(() => {
    const savedNotices = localStorage.getItem('notices');
    if (savedNotices) {
      setNotices(JSON.parse(savedNotices));
    }
  }, []);

  const deleteNotice = (id: number) => {
    if (confirm('정말로 이 공지를 삭제하시겠습니까?')) {
      const updated = notices.filter(n => n.id !== id);
      localStorage.setItem('notices', JSON.stringify(updated));
      setNotices(updated);
    }
  };

  const getCategoryLabel = (category: string) => {
    switch (category) {
      case 'urgent': return { label: '긴급', color: 'bg-red-100 text-red-700' };
      case 'event': return { label: '행사', color: 'bg-purple-100 text-purple-700' };
      default: return { label: '일반', color: 'bg-blue-100 text-blue-700' };
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b sticky top-0 z-50">
        <div className="max-w-5xl mx-auto px-6 py-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="p-2 hover:bg-gray-100 rounded-xl">
              <ArrowLeft className="w-6 h-6" />
            </Link>
            <h1 className="text-3xl font-bold">공지사항</h1>
          </div>
          <Link href="/notices/new">
            <Button className="flex items-center gap-2 rounded-2xl px-6 py-6">
              <Plus className="w-5 h-5" />
              공지 작성하기
            </Button>
          </Link>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-6 py-10">
        {notices.length === 0 ? (
          <div className="bg-white rounded-3xl p-20 text-center">
            <p className="text-gray-400 text-xl">등록된 공지사항이 없습니다.</p>
            <Link href="/notices/new">
              <Button className="mt-6">첫 공지 작성하기</Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {notices.map((notice) => {
              const cat = getCategoryLabel(notice.category);
              return (
                <div key={notice.id} className="bg-white rounded-3xl p-8 hover:shadow-md transition group">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        <span className={`px-4 py-1.5 text-sm font-medium rounded-2xl ${cat.color}`}>
                          {cat.label}
                        </span>
                        <span className="text-sm text-gray-500 flex items-center gap-1">
                          <Calendar className="w-4 h-4" />
                          {notice.date}
                        </span>
                      </div>
                      <h3 className="text-2xl font-bold mt-4 group-hover:text-blue-600 transition">
                        {notice.title}
                      </h3>
                      <p className="text-gray-600 mt-3 line-clamp-2">{notice.content}</p>
                    </div>
                  </div>

                  <div className="flex gap-3 mt-8">
                    <Link href={`/notices/${notice.id}`}>
                      <Button variant="outline" className="rounded-2xl">
                        <Eye className="w-4 h-4 mr-2" />
                        자세히 보기
                      </Button>
                    </Link>
                    <Button 
                      variant="ghost" 
                      className="text-red-500 hover:bg-red-50 rounded-2xl"
                      onClick={() => deleteNotice(notice.id)}
                    >
                      삭제
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}