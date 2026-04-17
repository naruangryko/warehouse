'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Bell, Calendar, Users, FolderOpen, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function DashboardPage() {
  const [clubName, setClubName] = useState("웨어바우스");
  const [noticeCount, setNoticeCount] = useState(0);
  const [scheduleCount, setScheduleCount] = useState(0);
  const [resourceCount, setResourceCount] = useState(0);
  const [recentNotices, setRecentNotices] = useState<any[]>([]);

  // 모든 데이터 불러오기
  const loadAllData = () => {
    // 공지사항 개수
    const notices = JSON.parse(localStorage.getItem('notices') || '[]');
    setNoticeCount(notices.length);
    setRecentNotices(notices.slice(0, 3));

    // 일정 개수
    const schedules = JSON.parse(localStorage.getItem('schedules') || '[]');
    setScheduleCount(schedules.length);

    // 자료실 개수
    const resources = JSON.parse(localStorage.getItem('resources') || '[]');
    setResourceCount(resources.length);
  };

  useEffect(() => {
    loadAllData();

    // localStorage 변경 감지
    const handleStorageChange = () => loadAllData();
    window.addEventListener('storage', handleStorageChange);
    
    // 페이지 focus 될 때도 새로고침
    window.addEventListener('focus', loadAllData);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('focus', loadAllData);
    };
  }, []);

  // 동아리 이름 불러오기
  useEffect(() => {
    const savedName = localStorage.getItem('clubName');
    if (savedName) setClubName(savedName);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 상단 헤더 */}
      <header className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-8 py-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-11 h-11 bg-blue-600 rounded-2xl flex items-center justify-center text-white font-bold text-3xl shadow">
              동
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{clubName}</h1>
              <p className="text-sm text-gray-500 -mt-0.5">Club Management System</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link href="/settings">
              <Button variant="outline" className="rounded-full px-6">
                ⚙️ 설정
              </Button>
            </Link>
            
            <div className="bg-gray-100 text-gray-700 px-5 py-2 rounded-full flex items-center gap-2 text-sm font-medium">
              <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
              멤버
            </div>
            
            <Button className="bg-gray-900 hover:bg-gray-800 text-white px-6 rounded-full">
              로그아웃 →
            </Button>
          </div>
        </div>
      </header>

      {/* 메인 컨텐츠 */}
      <div className="max-w-6xl mx-auto px-8 py-10">
        <div className="flex justify-between items-end mb-10">
          <div>
            <h2 className="text-4xl font-bold">대시보드</h2>
            <p className="text-gray-600 mt-2 text-lg">안녕하세요, 오늘도 함께해요 👋</p>
          </div>
          <p className="text-gray-500">2026년 4월 5일 토요일</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* 공지사항 */}
          <Link href="/notices" className="group">
            <div className="bg-white rounded-3xl p-8 hover:shadow-xl transition-all border border-blue-100 hover:border-blue-300">
              <div className="flex items-center justify-between">
                <div className="w-12 h-12 bg-blue-100 rounded-2xl flex items-center justify-center">
                  <Bell className="w-7 h-7 text-blue-600" />
                </div>
                {noticeCount > 0 && (
                  <div className="bg-red-500 text-white text-xs px-4 py-1 rounded-full font-medium">New</div>
                )}
              </div>
              <div className="mt-8">
                <p className="text-5xl font-bold text-gray-900">{noticeCount}</p>
                <p className="text-xl font-medium text-gray-700 mt-3">공지사항</p>
                <p className="text-gray-500 mt-2 text-sm">
                  {noticeCount > 0 ? '새로운 공지가 있습니다' : '등록된 공지가 없습니다'}
                </p>
              </div>
              <div className="mt-10 text-blue-600 font-medium flex items-center gap-2 group-hover:gap-3 transition">
                바로가기 <ArrowRight className="w-5 h-5" />
              </div>
            </div>
          </Link>

          {/* 다가오는 일정 - 이제 실시간으로 개수 반영 */}
          <Link href="/schedule" className="group">
            <div className="bg-white rounded-3xl p-8 hover:shadow-xl transition-all border border-emerald-100 hover:border-emerald-300">
              <div className="w-12 h-12 bg-emerald-100 rounded-2xl flex items-center justify-center">
                <Calendar className="w-7 h-7 text-emerald-600" />
              </div>
              <div className="mt-8">
                <p className="text-5xl font-bold text-gray-900">{scheduleCount}</p>
                <p className="text-xl font-medium text-gray-700 mt-3">다가오는 일정</p>
                <p className="text-gray-500 mt-2 text-sm">
                  {scheduleCount > 0 ? `${scheduleCount}개의 일정이 등록됨` : '등록된 일정이 없습니다'}
                </p>
              </div>
              <div className="mt-10 text-emerald-600 font-medium flex items-center gap-2 group-hover:gap-3 transition">
                바로가기 <ArrowRight className="w-5 h-5" />
              </div>
            </div>
          </Link>

          {/* 출석률 */}
          <Link href="/attendance" className="group">
            <div className="bg-white rounded-3xl p-8 hover:shadow-xl transition-all border border-amber-100 hover:border-amber-300">
              <div className="w-12 h-12 bg-amber-100 rounded-2xl flex items-center justify-center">
                <Users className="w-7 h-7 text-amber-600" />
              </div>
              <div className="mt-8">
                <p className="text-5xl font-bold text-amber-600">87%</p>
                <p className="text-xl font-medium text-gray-700 mt-3">이번 달 출석률</p>
                <p className="text-gray-500 mt-2 text-sm">12회 중 10회 참석</p>
              </div>
              <div className="mt-10 text-amber-600 font-medium flex items-center gap-2 group-hover:gap-3 transition">
                바로가기 <ArrowRight className="w-5 h-5" />
              </div>
            </div>
          </Link>

          {/* 자료실 - 이제 제대로 개수 반영 */}
          <Link href="/resources" className="group">
            <div className="bg-white rounded-3xl p-8 hover:shadow-xl transition-all border border-purple-100 hover:border-purple-300">
              <div className="w-12 h-12 bg-purple-100 rounded-2xl flex items-center justify-center">
                <FolderOpen className="w-7 h-7 text-purple-600" />
              </div>
              <div className="mt-8">
                <p className="text-5xl font-bold text-gray-900">{resourceCount}</p>
                <p className="text-xl font-medium text-gray-700 mt-3">자료실</p>
                <p className="text-gray-500 mt-2 text-sm">
                  {resourceCount > 0 
                    ? `총 ${resourceCount}개의 자료가 있습니다` 
                    : '아직 등록된 자료가 없습니다'}
                </p>
              </div>
              <div className="mt-10 text-purple-600 font-medium flex items-center gap-2 group-hover:gap-3 transition">
                바로가기 <ArrowRight className="w-5 h-5" />
              </div>
            </div>
          </Link>
        </div>

        {/* 최근 공지사항 */}
        <div className="mt-16">
          <div className="flex justify-between items-center mb-6 px-1">
            <h2 className="text-2xl font-bold">최근 공지사항</h2>
            <Link href="/notices" className="text-blue-600 hover:underline text-sm flex items-center gap-1">
              모두 보기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="bg-white rounded-3xl p-2">
            {recentNotices.length > 0 ? (
              recentNotices.map((notice) => (
                <Link
                  key={notice.id}
                  href={`/notices/${notice.id}`}
                  className="block px-8 py-6 hover:bg-gray-50 rounded-3xl transition"
                >
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="font-semibold text-lg">{notice.title}</p>
                      <p className="text-sm text-gray-500 mt-1">{notice.date}</p>
                    </div>
                    <span className="text-blue-600 text-sm font-medium">자세히 보기 →</span>
                  </div>
                </Link>
              ))
            ) : (
              <div className="py-16 text-center text-gray-400">
                등록된 공지사항이 없습니다.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}