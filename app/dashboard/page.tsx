'use client';

import Link from 'next/link';
import { Users, Calendar, FolderOpen, TrendingUp, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useEffect, useState } from 'react';
import { supabase } from '@/lib/supabase';

export default function DashboardPage() {
  const [stats, setStats] = useState({
    members: 0,
    resources: 0,
    attendanceRate: 0,
  });

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    // 멤버 수
    const { count: memberCount } = await supabase
      .from('members')
      .select('*', { count: 'exact' });

    // 자료 수
    const { count: resourceCount } = await supabase
      .from('resources')
      .select('*', { count: 'exact' });

    // 출석률 (present인 사람 비율)
    const { data: attendanceData } = await supabase
      .from('members')
      .select('status');

    let attendanceRate = 0;
    if (attendanceData && attendanceData.length > 0) {
      const presentCount = attendanceData.filter(m => m.status === 'present').length;
      attendanceRate = Math.round((presentCount / attendanceData.length) * 100);
    }

    setStats({
      members: memberCount || 0,
      resources: resourceCount || 0,
      attendanceRate: attendanceRate,
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      <div className="max-w-6xl mx-auto px-8 pt-10">
        <div className="flex justify-between items-end mb-10">
          <div>
            <h1 className="text-4xl font-bold">동아리 관리 시스템</h1>
            <p className="text-gray-600 mt-2 text-lg">WearBaus Club Management</p>
          </div>
          <Button 
            onClick={() => window.location.href = '/api/init-supabase'}
            variant="outline"
            className="text-sm"
          >
            Supabase 테이블 초기화
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="bg-white rounded-3xl p-8">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">등록된 멤버</p>
                <p className="text-5xl font-bold mt-3">{stats.members}명</p>
              </div>
              <Users className="w-12 h-12 text-blue-500" />
            </div>
            <Link href="/attendance" className="text-blue-600 text-sm flex items-center gap-1 mt-6 hover:underline">
              출석 관리하기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="bg-white rounded-3xl p-8">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">자료실 등록 수</p>
                <p className="text-5xl font-bold mt-3">{stats.resources}개</p>
              </div>
              <FolderOpen className="w-12 h-12 text-purple-500" />
            </div>
            <Link href="/resources" className="text-purple-600 text-sm flex items-center gap-1 mt-6 hover:underline">
              자료실 바로가기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="bg-white rounded-3xl p-8">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">이번 달 출석률</p>
                <p className="text-5xl font-bold mt-3 text-amber-600">{stats.attendanceRate}%</p>
              </div>
              <TrendingUp className="w-12 h-12 text-amber-500" />
            </div>
            <Link href="/attendance" className="text-amber-600 text-sm flex items-center gap-1 mt-6 hover:underline">
              출석 체크하기 <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Link href="/schedule" className="block group">
            <div className="bg-white rounded-3xl p-10 hover:shadow-xl transition-all">
              <Calendar className="w-12 h-12 text-indigo-500 mb-6" />
              <h2 className="text-2xl font-bold">일정 관리</h2>
              <p className="text-gray-600 mt-3 leading-relaxed">
                동아리 일정을 등록하고<br />관리할 수 있습니다.
              </p>
              <div className="mt-8 text-indigo-600 font-medium flex items-center gap-2 group-hover:gap-3 transition-all">
                바로가기 →
              </div>
            </div>
          </Link>

          <Link href="/resources" className="block group">
            <div className="bg-white rounded-3xl p-10 hover:shadow-xl transition-all">
              <FolderOpen className="w-12 h-12 text-teal-500 mb-6" />
              <h2 className="text-2xl font-bold">자료실</h2>
              <p className="text-gray-600 mt-3 leading-relaxed">
                회의록, 활동사진, 홍보물 등을<br />안전하게 보관하고 공유합니다.
              </p>
              <div className="mt-8 text-teal-600 font-medium flex items-center gap-2 group-hover:gap-3 transition-all">
                바로가기 →
              </div>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}