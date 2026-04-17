'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Save, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export default function SettingsPage() {
  const [clubName, setClubName] = useState('');
  const [isSaved, setIsSaved] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // 페이지 로드 시 localStorage에서 동아리 이름 불러오기
  useEffect(() => {
    const savedName = localStorage.getItem('clubName');
    if (savedName) {
      setClubName(savedName);
    } else {
      setClubName('웨어바우스'); // 기본값
    }
  }, []);

  const handleSave = () => {
    if (!clubName.trim()) {
      alert('동아리 이름을 입력해주세요.');
      return;
    }

    localStorage.setItem('clubName', clubName.trim());
    setIsSaved(true);
    setIsEditing(false);

    // 2초 후에 저장 메시지 사라지게
    setTimeout(() => {
      setIsSaved(false);
    }, 2000);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <header className="bg-white border-b sticky top-0 z-50">
        <div className="max-w-4xl mx-auto px-8 py-5 flex items-center gap-4">
          <Link href="/dashboard" className="p-2 hover:bg-gray-100 rounded-xl transition">
            <ArrowLeft className="w-6 h-6" />
          </Link>
          <div>
            <h1 className="text-3xl font-bold">설정</h1>
            <p className="text-gray-500">동아리 정보를 관리하세요</p>
          </div>
        </div>
      </header>

      <div className="max-w-4xl mx-auto px-8 py-12">
        <div className="bg-white rounded-3xl shadow-sm p-10">
          <h2 className="text-2xl font-bold mb-8">동아리 기본 정보</h2>

          <div className="space-y-10">
            {/* 동아리 이름 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-3">
                동아리 이름
              </label>
              
              <div className="flex gap-4">
                <Input
                  value={clubName}
                  onChange={(e) => setClubName(e.target.value)}
                  placeholder="동아리 이름을 입력하세요"
                  className="text-xl py-7 rounded-2xl"
                  disabled={!isEditing}
                />
                
                <Button
                  onClick={() => setIsEditing(!isEditing)}
                  variant={isEditing ? "outline" : "default"}
                  className="px-8 rounded-2xl"
                >
                  {isEditing ? '취소' : '수정'}
                </Button>
              </div>
            </div>

            {/* 저장 버튼 */}
            <div className="pt-6 border-t">
              <Button 
                onClick={handleSave}
                disabled={!isEditing}
                className="w-full py-7 text-lg rounded-2xl flex items-center justify-center gap-3"
              >
                <Save className="w-6 h-6" />
                변경사항 저장하기
              </Button>
            </div>

            {/* 저장 완료 메시지 */}
            {isSaved && (
              <div className="flex items-center justify-center gap-3 text-green-600 bg-green-50 py-4 rounded-2xl">
                <Check className="w-6 h-6" />
                <span className="font-medium">동아리 이름이 성공적으로 변경되었습니다!</span>
              </div>
            )}
          </div>
        </div>

        <div className="mt-8 text-center text-gray-400 text-sm">
          변경된 동아리 이름은 대시보드와 모든 페이지에 실시간으로 반영됩니다.
        </div>
      </div>
    </div>
  );
}