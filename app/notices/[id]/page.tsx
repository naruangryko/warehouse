'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft, Calendar, Edit, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Notice = {
  id: number;
  title: string;
  content: string;
  date: string;
  category: 'general' | 'event' | 'urgent';
};

export default function NoticeDetailPage() {
  const router = useRouter();
  const params = useParams();
  const id = parseInt(params.id as string);

  const [notice, setNotice] = useState<Notice | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const savedNotices = localStorage.getItem('notices');
    if (savedNotices) {
      const notices: Notice[] = JSON.parse(savedNotices);
      const foundNotice = notices.find(n => n.id === id);
      
      if (foundNotice) {
        setNotice(foundNotice);
      } else {
        alert('해당 공지사항을 찾을 수 없습니다.');
        router.push('/notices');
      }
    } else {
      router.push('/notices');
    }
    setIsLoading(false);
  }, [id, router]);

  const deleteNotice = () => {
    if (!confirm('정말로 이 공지사항을 삭제하시겠습니까?')) return;

    const savedNotices = localStorage.getItem('notices');
    if (savedNotices) {
      const notices: Notice[] = JSON.parse(savedNotices);
      const updatedNotices = notices.filter(n => n.id !== id);
      
      localStorage.setItem('notices', JSON.stringify(updatedNotices));
      alert('공지사항이 삭제되었습니다.');
      router.push('/notices');
    }
  };

  const getCategoryLabel = (category: string) => {
    switch (category) {
      case 'urgent': return { label: '긴급', color: 'bg-red-100 text-red-700 border-red-200' };
      case 'event': return { label: '행사', color: 'bg-purple-100 text-purple-700 border-purple-200' };
      default: return { label: '일반', color: 'bg-blue-100 text-blue-700 border-blue-200' };
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-xl text-gray-500">로딩 중...</p>
      </div>
    );
  }

  if (!notice) {
    return null;
  }

  const categoryInfo = getCategoryLabel(notice.category);

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b sticky top-0 z-50">
        <div className="max-w-4xl mx-auto px-8 py-5 flex items-center gap-4">
          <Link href="/notices" className="p-2 hover:bg-gray-100 rounded-xl transition">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-2xl font-bold">공지사항</h1>
        </div>
      </header>

      <div className="max-w-4xl mx-auto px-8 py-12">
        <div className="bg-white rounded-3xl shadow-sm overflow-hidden">
          {/* 상단 정보 */}
          <div className="px-10 pt-10 pb-6 border-b">
            <div className="flex items-center gap-4">
              <span className={`px-6 py-2 rounded-2xl text-sm font-semibold ${categoryInfo.color}`}>
                {categoryInfo.label}
              </span>
              <div className="flex items-center gap-2 text-gray-500 text-sm">
                <Calendar className="w-4 h-4" />
                {notice.date}
              </div>
            </div>

            <h1 className="text-4xl font-bold mt-6 leading-tight">
              {notice.title}
            </h1>
          </div>

          {/* 본문 */}
          <div className="px-10 py-12 leading-relaxed text-lg text-gray-700 whitespace-pre-wrap">
            {notice.content}
          </div>

          {/* 하단 버튼 */}
          <div className="px-10 py-8 bg-gray-50 border-t flex gap-4">
            <Link href="/notices">
              <Button variant="outline" className="rounded-2xl px-8 py-6">
                목록으로 돌아가기
              </Button>
            </Link>

            <Button 
              onClick={deleteNotice}
              variant="destructive"
              className="rounded-2xl px-8 py-6 flex items-center gap-2"
            >
              <Trash2 className="w-5 h-5" />
              삭제하기
            </Button>
          </div>
        </div>

        <div className="text-center text-gray-400 text-sm mt-8">
          이 페이지는 localStorage에 저장된 데이터를 기반으로 표시됩니다.
        </div>
      </div>
    </div>
  );
}