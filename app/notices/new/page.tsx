'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type Category = 'general' | 'event' | 'urgent';

export default function NewNoticePage() {
  const router = useRouter();
  
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState<Category>('general');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim() || !content.trim()) {
      alert('제목과 내용을 모두 입력해주세요.');
      return;
    }

    const newNotice = {
      id: Date.now(),
      title: title.trim(),
      content: content.trim(),
      date: new Date().toISOString().split('T')[0],
      category,
    };

    // 기존 공지사항 불러오기
    const existingNotices = JSON.parse(localStorage.getItem('notices') || '[]');
    const updatedNotices = [newNotice, ...existingNotices];
    
    localStorage.setItem('notices', JSON.stringify(updatedNotices));

    alert('공지사항이 성공적으로 등록되었습니다!');
    router.push('/notices');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b sticky top-0 z-50">
        <div className="max-w-4xl mx-auto px-8 py-5 flex items-center gap-4">
          <Link href="/notices" className="p-2 hover:bg-gray-100 rounded-xl transition">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <h1 className="text-3xl font-bold">새 공지 작성</h1>
        </div>
      </header>

      <div className="max-w-4xl mx-auto px-8 py-12">
        <form onSubmit={handleSubmit} className="bg-white rounded-3xl shadow-sm p-10">
          <div className="space-y-8">
            {/* 카테고리 선택 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">카테고리</label>
              <div className="flex gap-3">
                {[
                  { value: 'general', label: '일반', color: 'bg-blue-100 text-blue-700' },
                  { value: 'event', label: '행사', color: 'bg-purple-100 text-purple-700' },
                  { value: 'urgent', label: '긴급', color: 'bg-red-100 text-red-700' },
                ].map((cat) => (
                  <button
                    key={cat.value}
                    type="button"
                    onClick={() => setCategory(cat.value as Category)}
                    className={`px-6 py-3 rounded-2xl text-sm font-medium transition-all ${
                      category === cat.value 
                        ? cat.color + ' ring-2 ring-offset-2 ring-blue-500' 
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {cat.label}
                  </button>
                ))}
              </div>
            </div>

            {/* 제목 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">제목</label>
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="공지사항 제목을 입력하세요"
                className="text-xl py-7 rounded-2xl"
                required
              />
            </div>

            {/* 내용 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">내용</label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="공지사항 내용을 입력하세요..."
                className="w-full h-80 px-6 py-5 text-lg border border-gray-200 rounded-3xl focus:outline-none focus:border-blue-500 resize-y min-h-[300px]"
                required
              />
            </div>

            {/* 등록 버튼 */}
            <div className="pt-6">
              <Button 
                type="submit"
                className="w-full py-7 text-lg rounded-2xl font-medium"
              >
                공지사항 등록하기
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}