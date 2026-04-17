'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ArrowLeft, Plus, Trash2, Download, CheckCircle, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Member = {
  id: number;
  studentId: string;
  name: string;
  status: 'present' | 'absent' | 'unknown';
};

export default function AttendancePage() {
  const [members, setMembers] = useState<Member[]>([]);
  const [newStudentId, setNewStudentId] = useState('');
  const [newName, setNewName] = useState('');
  const [date] = useState("2026-04-05");

  // localStorage에서 데이터 불러오기
  useEffect(() => {
    const savedMembers = localStorage.getItem('members');
    const savedAttendance = localStorage.getItem('attendance');

    if (savedMembers) {
      const parsedMembers = JSON.parse(savedMembers);
      if (savedAttendance) {
        const attendanceMap = new Map(JSON.parse(savedAttendance));
        const merged = parsedMembers.map((m: any) => ({
          ...m,
          status: attendanceMap.get(m.id) || 'unknown'
        }));
        setMembers(merged);
      } else {
        setMembers(parsedMembers.map((m: any) => ({ ...m, status: 'unknown' })));
      }
    } else {
      // 초기 데이터
      const initialMembers: Member[] = [
        { id: 1, studentId: "20230001", name: "김민준", status: "unknown" },
        { id: 2, studentId: "20230002", name: "이서연", status: "unknown" },
        { id: 3, studentId: "20230003", name: "박지훈", status: "unknown" },
        { id: 4, studentId: "20230004", name: "최수아", status: "unknown" },
      ];
      setMembers(initialMembers);
      localStorage.setItem('members', JSON.stringify(initialMembers));
    }
  }, []);

  const saveData = (updatedMembers: Member[]) => {
    setMembers(updatedMembers);
    localStorage.setItem('members', JSON.stringify(updatedMembers.map(m => ({ id: m.id, studentId: m.studentId, name: m.name }))));
    
    const attendanceData = updatedMembers.map(m => [m.id, m.status]);
    localStorage.setItem('attendance', JSON.stringify(attendanceData));
  };

  const addMember = () => {
    if (!newStudentId || !newName) {
      alert("학번과 이름을 모두 입력해주세요.");
      return;
    }

    const newMember: Member = {
      id: Date.now(),
      studentId: newStudentId,
      name: newName,
      status: 'unknown'
    };

    const updated = [...members, newMember];
    saveData(updated);
    setNewStudentId('');
    setNewName('');
  };

  const deleteMember = (id: number) => {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    const updated = members.filter(m => m.id !== id);
    saveData(updated);
  };

  const updateStatus = (id: number, status: 'present' | 'absent') => {
    const updated = members.map(member =>
      member.id === id ? { ...member, status } : member
    );
    saveData(updated);
  };

  const presentCount = members.filter(m => m.status === 'present').length;
  const attendanceRate = members.length > 0 
    ? Math.round((presentCount / members.length) * 100) 
    : 0;

  // 엑셀 다운로드 기능
  const downloadExcel = () => {
    let csvContent = "학번,이름,출석여부\n";
    
    members.forEach(member => {
      const statusText = member.status === 'present' ? '출석' : 
                        member.status === 'absent' ? '결석' : '미입력';
      csvContent += `${member.studentId},${member.name},${statusText}\n`;
    });

    const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.download = `출석부_${date}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    
    alert("출석 결과가 다운로드되었습니다. (엑셀에서 열 수 있습니다)");
  };

  const resetAll = () => {
    const resetMembers = members.map(member => ({ ...member, status: 'unknown' }));
    saveData(resetMembers);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-8 py-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="p-2 hover:bg-gray-100 rounded-xl">
              <ArrowLeft className="w-6 h-6" />
            </Link>
            <div>
              <h1 className="text-3xl font-bold">출석 관리</h1>
              <p className="text-gray-500">{date} • 총 {members.length}명</p>
            </div>
          </div>
          
          <div className="flex gap-3">
            <Button variant="outline" onClick={resetAll}>전체 초기화</Button>
            <Button onClick={downloadExcel} className="flex items-center gap-2">
              <Download className="w-5 h-5" />
              엑셀 다운로드
            </Button>
          </div>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-8 py-10">
        {/* 출석률 요약 */}
        <div className="bg-white rounded-3xl p-10 mb-10">
          <div className="flex justify-between items-center">
            <div>
              <p className="text-7xl font-bold text-amber-600">{attendanceRate}%</p>
              <p className="text-2xl text-gray-600 mt-3">이번 달 출석률</p>
            </div>
            <div className="text-right">
              <p className="text-5xl font-semibold text-gray-800">{presentCount} / {members.length}</p>
              <p className="text-xl text-gray-500">명 출석</p>
            </div>
          </div>
        </div>

        {/* 멤버 추가 */}
        <div className="bg-white rounded-3xl p-8 mb-8">
          <h2 className="text-xl font-bold mb-4">새 멤버 추가</h2>
          <div className="flex gap-4">
            <input
              type="text"
              placeholder="학번 (예: 20230005)"
              value={newStudentId}
              onChange={(e) => setNewStudentId(e.target.value)}
              className="flex-1 px-5 py-4 border rounded-2xl focus:outline-none"
            />
            <input
              type="text"
              placeholder="이름"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="flex-1 px-5 py-4 border rounded-2xl focus:outline-none"
            />
            <Button onClick={addMember} className="px-10 rounded-2xl">
              <Plus className="w-5 h-5 mr-2" />
              추가
            </Button>
          </div>
        </div>

        {/* 출석부 테이블 */}
        <div className="bg-white rounded-3xl overflow-hidden">
          <div className="grid grid-cols-12 bg-gray-50 px-8 py-5 text-sm font-medium text-gray-500 border-b">
            <div className="col-span-2">학번</div>
            <div className="col-span-4">이름</div>
            <div className="col-span-6 text-center">출석 체크</div>
          </div>

          {members.map((member) => (
            <div key={member.id} className="grid grid-cols-12 px-8 py-6 border-b last:border-none items-center hover:bg-gray-50 group">
              <div className="col-span-2 font-mono text-gray-600">{member.studentId}</div>
              <div className="col-span-4 font-semibold">{member.name}</div>
              
              <div className="col-span-5 flex justify-center gap-6">
                <button
                  onClick={() => updateStatus(member.id, 'present')}
                  className={`flex items-center gap-3 px-8 py-3 rounded-2xl transition-all ${
                    member.status === 'present' 
                      ? 'bg-green-100 text-green-700 ring-2 ring-green-500' 
                      : 'hover:bg-gray-100 text-gray-600'
                  }`}
                >
                  <CheckCircle className="w-6 h-6" />
                  출석
                </button>

                <button
                  onClick={() => updateStatus(member.id, 'absent')}
                  className={`flex items-center gap-3 px-8 py-3 rounded-2xl transition-all ${
                    member.status === 'absent' 
                      ? 'bg-red-100 text-red-700 ring-2 ring-red-500' 
                      : 'hover:bg-gray-100 text-gray-600'
                  }`}
                >
                  <XCircle className="w-6 h-6" />
                  결석
                </button>
              </div>

              <div className="col-span-1 flex justify-end">
                <button
                  onClick={() => deleteMember(member.id)}
                  className="text-red-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition p-2"
                >
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="text-center text-sm text-gray-500 mt-8">
          출석 체크 후 <span className="font-medium">엑셀 다운로드</span> 버튼을 누르면 CSV 파일로 저장됩니다.
        </div>
      </div>
    </div>
  );
}