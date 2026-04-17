// types/index.ts
export type Status = 'present' | 'absent' | 'unknown';

export interface Member {
  id: number;
  studentId: string;
  name: string;
  status: Status | string;           // Supabase와 호환되도록 string 허용
  created_at?: string;
}

export interface Resource {
  id: number;
  title: string;
  category: string;
  description?: string;
  file_name: string;
  file_size: string;
  file_type: string;
  file_url: string;
  created_at?: string;
}

export interface AttendanceRecord {
  id?: number;
  date: string;
  total_members: number;
  present_count: number;
  absent_count: number;
  created_at?: string;
}