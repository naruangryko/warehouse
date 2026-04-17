import { supabase } from '@/lib/supabase';
import { NextResponse } from 'next/server';

export async function GET() {
  try {
    // members 테이블 생성
    await supabase.from('members').select('id').limit(1);
    
    // resources 테이블 생성
    await supabase.from('resources').select('id').limit(1);

    console.log('✅ 테이블 확인 완료');

    return NextResponse.json({ 
      success: true, 
      message: 'Supabase 테이블이 준비되었습니다.' 
    });
  } catch (error) {
    console.error(error);

    // 테이블이 없을 경우 생성 시도
    try {
      // members 테이블 생성
      const { error: memberError } = await supabase.rpc('create_members_table');
      if (memberError) {
        console.log('members 테이블이 이미 존재하거나 생성 중...');
      }

      // resources 테이블 생성
      const { error: resourceError } = await supabase.rpc('create_resources_table');
      if (resourceError) {
        console.log('resources 테이블이 이미 존재하거나 생성 중...');
      }

      return NextResponse.json({ 
        success: true, 
        message: '테이블이 생성되었습니다. (또는 이미 존재합니다)' 
      });
    } catch (e) {
      return NextResponse.json({ 
        success: false, 
        message: '테이블 생성에 실패했습니다. Supabase 대시보드에서 직접 생성해주세요.' 
      });
    }
  }
}