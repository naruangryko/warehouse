// app/api/upload/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { writeFile } from 'fs/promises';
import path from 'path';

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData();
    const file = formData.get('file') as File;
    const category = formData.get('category') as string;
    const title = formData.get('title') as string;

    if (!file) {
      return NextResponse.json({ error: '파일이 없습니다.' }, { status: 400 });
    }

    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);

    const filename = Date.now() + '_' + file.name.replace(/\s+/g, '_');
    const uploadDir = path.join(process.cwd(), 'public', 'uploads');
    
    // 디렉토리가 없으면 생성
    const fs = require('fs');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const filepath = path.join(uploadDir, filename);
    await writeFile(filepath, buffer);

    const fileUrl = `/uploads/${filename}`;
    const fileType = file.name.split('.').pop()?.toLowerCase() || '';
    const isImage = ['jpg', 'jpeg', 'png', 'gif'].includes(fileType);

    return NextResponse.json({
      success: true,
      title: title || file.name.replace(/\.[^/.]+$/, ""),
      category,
      fileName: file.name,
      fileType,
      uploadDate: new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
      size: (file.size / (1024 * 1024)).toFixed(1) + ' MB',
      previewUrl: isImage ? fileUrl : undefined,
      isImage,
      fileUrl,
    });
  } catch (error) {
    console.error(error);
    return NextResponse.json({ error: '업로드 실패' }, { status: 500 });
  }
}