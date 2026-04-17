'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Plus, Trash2, ChevronLeft, ChevronRight, Calendar as CalendarIcon, Clock, MapPin } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Schedule = {
  id: number;
  title: string;
  date: string;        // YYYY-MM-DD
  startTime: string;
  endTime: string;
  location: string;
  description: string;
  category: 'meeting' | 'event' | 'etc';
};

export default function SchedulePage() {
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [currentDate, setCurrentDate] = useState(new Date(2026, 3, 1)); // 2026년 4월 시작

  // 폼 상태
  const [formData, setFormData] = useState({
    title: '',
    date: '2026-04-16',
    startTime: '15:00',
    endTime: '17:00',
    location: '',
    description: '',
    category: 'meeting' as 'meeting' | 'event' | 'etc',
  });

  // localStorage에서 일정 불러오기
  useEffect(() => {
    const saved = localStorage.getItem('schedules');
    if (saved) {
      setSchedules(JSON.parse(saved));
    } else {
      const initialData: Schedule[] = [
        {
          id: 1,
          title: "4월 정기 월례회",
          date: "2026-04-12",
          startTime: "15:00",
          endTime: "17:00",
          location: "대학교 카페 블루문 2층",
          description: "이번 달 활동 계획 공유 및 팀별 진행 상황 보고",
          category: "meeting",
        },
        {
          id: 2,
          title: "신입 회원 환영회",
          date: "2026-04-19",
          startTime: "17:30",
          endTime: "21:00",
          location: "홍대 맛집 - 불타는족발",
          description: "신입 회원들과의 친목 도모 및 ice-breaking",
          category: "event",
        },
      ];
      setSchedules(initialData);
      localStorage.setItem('schedules', JSON.stringify(initialData));
    }
  }, []);

  const saveSchedules = (newSchedules: Schedule[]) => {
    setSchedules(newSchedules);
    localStorage.setItem('schedules', JSON.stringify(newSchedules));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim()) return;

    const newSchedule: Schedule = {
      id: Date.now(),
      ...formData,
    };

    const updated = [newSchedule, ...schedules];
    saveSchedules(updated);
    setShowForm(false);
    setFormData({
      title: '',
      date: '2026-04-16',
      startTime: '15:00',
      endTime: '17:00',
      location: '',
      description: '',
      category: 'meeting',
    });
    alert('일정이 등록되었습니다!');
  };

  const deleteSchedule = (id: number) => {
    if (!confirm('정말로 삭제하시겠습니까?')) return;
    const updated = schedules.filter(s => s.id !== id);
    saveSchedules(updated);
  };

  // 달력 관련 함수
  const prevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const getDaysInMonth = (date: Date) => {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date: Date) => {
    return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
  };

  // 일정이 있는 날짜 체크
  const hasSchedule = (year: number, month: number, day: number) => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    return schedules.some(schedule => schedule.date === dateStr);
  };

  // 오늘 날짜 체크
  const isToday = (year: number, month: number, day: number) => {
    const today = new Date();
    return (
      today.getFullYear() === year &&
      today.getMonth() === month &&
      today.getDate() === day
    );
  };

  const generateCalendar = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const daysInMonth = getDaysInMonth(currentDate);
    const firstDay = getFirstDayOfMonth(currentDate);
    const days = [];

    // 빈 칸
    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`empty-${i}`} className="h-12"></div>);
    }

    // 날짜
    for (let day = 1; day <= daysInMonth; day++) {
      const hasEvent = hasSchedule(year, month, day);
      const today = isToday(year, month, day);

      days.push(
        <div
          key={day}
          className={`h-12 flex flex-col items-center justify-center relative text-sm rounded-2xl cursor-pointer hover:bg-gray-100 transition
            ${today ? 'bg-blue-600 text-white font-bold' : 'text-gray-700'}`}
        >
          {day}
          {hasEvent && (
            <div className="absolute bottom-2 w-1.5 h-1.5 bg-red-500 rounded-full"></div>
          )}
        </div>
      );
    }
    return days;
  };

  const getCategoryColor = (category: string) => {
    if (category === 'meeting') return 'bg-blue-100 text-blue-700';
    if (category === 'event') return 'bg-purple-100 text-purple-700';
    return 'bg-gray-100 text-gray-600';
  };

  const getCategoryLabel = (category: string) => {
    if (category === 'meeting') return '정기회의';
    if (category === 'event') return '행사';
    return '기타';
  };

  const monthName = currentDate.toLocaleString('ko-KR', { month: 'long' });

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-8 py-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="p-2 hover:bg-gray-100 rounded-xl">
              <ArrowLeft className="w-6 h-6" />
            </Link>
            <div>
              <h1 className="text-3xl font-bold">다가오는 일정</h1>
              <p className="text-gray-500">총 {schedules.length}개의 일정이 등록되어 있습니다</p>
            </div>
          </div>

          <Button onClick={() => setShowForm(true)} className="rounded-full px-6 py-6 flex items-center gap-2">
            <Plus className="w-5 h-5" />
            일정 등록하기
          </Button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-1 lg:grid-cols-7 gap-8">
        {/* 달력 */}
        <div className="lg:col-span-3">
          <div className="bg-white rounded-3xl p-8 sticky top-6">
            <div className="flex items-center justify-between mb-6">
              <button onClick={prevMonth} className="p-2 hover:bg-gray-100 rounded-xl">
                <ChevronLeft className="w-6 h-6" />
              </button>
              <div className="flex items-center gap-3">
                <CalendarIcon className="w-6 h-6 text-blue-600" />
                <h2 className="text-2xl font-bold">{currentDate.getFullYear()}년 {monthName}</h2>
              </div>
              <button onClick={nextMonth} className="p-2 hover:bg-gray-100 rounded-xl">
                <ChevronRight className="w-6 h-6" />
              </button>
            </div>

            <div className="grid grid-cols-7 gap-1 text-center text-xs text-gray-400 mb-4">
              {['일', '월', '화', '수', '목', '금', '토'].map(day => (
                <div key={day} className="font-medium">{day}</div>
              ))}
            </div>

            <div className="grid grid-cols-7 gap-1">
              {generateCalendar()}
            </div>

            <div className="mt-10 pt-6 border-t">
              <h3 className="font-medium mb-4 text-gray-600">이번 달 일정 요약</h3>
              {schedules
                .filter(s => s.date.startsWith(`${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`))
                .slice(0, 5)
                .map(schedule => (
                  <div key={schedule.id} className="flex gap-3 text-sm py-3 border-b last:border-none">
                    <div className="w-16 text-gray-500 font-medium">
                      {schedule.date.slice(5)}
                    </div>
                    <div className="flex-1">
                      <p className="font-medium">{schedule.title}</p>
                      <p className="text-xs text-gray-500">{schedule.startTime} ~ {schedule.endTime}</p>
                    </div>
                  </div>
                ))}
            </div>
          </div>
        </div>

        {/* 일정 목록 */}
        <div className="lg:col-span-4 space-y-6">
          {schedules.length === 0 ? (
            <div className="bg-white rounded-3xl p-20 text-center text-gray-400">
              등록된 일정이 없습니다.
            </div>
          ) : (
            schedules.map(schedule => {
              const catColor = getCategoryColor(schedule.category);
              const catLabel = getCategoryLabel(schedule.category);

              return (
                <div key={schedule.id} className="bg-white rounded-3xl p-8 group">
                  <div className="flex justify-between items-start">
                    <div className="flex gap-4">
                      <span className={`px-5 py-2 rounded-2xl text-sm font-medium ${catColor}`}>
                        {catLabel}
                      </span>
                      <div className="flex items-center gap-2 text-gray-500 text-sm">
                        <Clock className="w-4 h-4" />
                        {schedule.startTime} ~ {schedule.endTime}
                      </div>
                    </div>
                    <button
                      onClick={() => deleteSchedule(schedule.id)}
                      className="text-red-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>

                  <h3 className="text-2xl font-bold mt-5">{schedule.title}</h3>

                  <div className="flex items-center gap-2 mt-4 text-gray-600">
                    <MapPin className="w-4 h-4" />
                    {schedule.location}
                  </div>

                  <p className="mt-6 text-gray-700 leading-relaxed whitespace-pre-wrap">
                    {schedule.description}
                  </p>

                  <div className="mt-8 text-sm text-gray-500">
                    {schedule.date.replace(/-/g, '.')} • {schedule.startTime}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* 일정 등록 모달 */}
      {showForm && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-3xl w-full max-w-2xl max-h-[90vh] overflow-auto">
            <div className="p-10">
              <h2 className="text-3xl font-bold mb-8">새 일정 등록</h2>

              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-sm font-medium mb-2">일정 제목</label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full px-5 py-4 border rounded-2xl text-lg"
                    placeholder="예: 5월 정기 월례회"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium mb-2">날짜</label>
                    <input
                      type="date"
                      value={formData.date}
                      onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                      className="w-full px-5 py-4 border rounded-2xl"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-2">카테고리</label>
                    <select
                      value={formData.category}
                      onChange={(e) => setFormData({ ...formData, category: e.target.value as any })}
                      className="w-full px-5 py-4 border rounded-2xl"
                    >
                      <option value="meeting">정기회의</option>
                      <option value="event">행사</option>
                      <option value="etc">기타</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium mb-2">시작 시간</label>
                    <input
                      type="time"
                      value={formData.startTime}
                      onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                      className="w-full px-5 py-4 border rounded-2xl"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-2">종료 시간</label>
                    <input
                      type="time"
                      value={formData.endTime}
                      onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                      className="w-full px-5 py-4 border rounded-2xl"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">장소</label>
                  <input
                    type="text"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="w-full px-5 py-4 border rounded-2xl"
                    placeholder="장소를 입력하세요"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">설명</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full h-32 px-5 py-4 border rounded-3xl resize-y"
                    placeholder="일정에 대한 상세 설명을 적어주세요"
                  />
                </div>

                <div className="flex gap-4 pt-6">
                  <Button 
                    type="button" 
                    variant="outline" 
                    className="flex-1 py-7 rounded-2xl"
                    onClick={() => setShowForm(false)}
                  >
                    취소
                  </Button>
                  <Button type="submit" className="flex-1 py-7 rounded-2xl">
                    일정 등록하기
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}