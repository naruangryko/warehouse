'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, Plus, Trash2, Download, Upload, FileText, Image, File, X } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Resource = {
  id: number;
  title: string;
  category: string;
  description: string;
  fileName: string;
  fileSize: string;
  fileType: 'pdf' | 'image' | 'doc' | 'etc';
  uploadDate: string;
  fileData?: string;        // Base64로 저장 (실제 다운로드용)
};

const categories = ["회의록", "활동사진", "홍보물", "자료집", "기타"];

export default function ResourcesPage() {
  const [resources, setResources] = useState<Resource[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [formData, setFormData] = useState({
    title: '',
    category: '회의록',
    description: '',
    file: null as File | null,
    fileName: '',
    fileSize: '',
    fileType: 'pdf' as 'pdf' | 'image' | 'doc' | 'etc',
  });

  useEffect(() => {
    const saved = localStorage.getItem('resources');
    if (saved) setResources(JSON.parse(saved));
  }, []);

  const saveResources = (newResources: Resource[]) => {
    setResources(newResources);
    localStorage.setItem('resources', JSON.stringify(newResources));
  };

  const getFileType = (file: File): 'pdf' | 'image' | 'doc' | 'etc' => {
    if (file.type.includes('pdf')) return 'pdf';
    if (file.type.includes('image')) return 'image';
    if (file.type.includes('word') || file.type.includes('document')) return 'doc';
    return 'etc';
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const handleFileSelect = (file: File) => {
    const type = getFileType(file);
    
    setFormData({
      ...formData,
      file,
      fileName: file.name,
      fileSize: formatFileSize(file.size),
      fileType: type,
      title: formData.title || file.name.replace(/\.[^/.]+$/, ""), // 확장자 제거
    });
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const addResource = async () => {
    if (!formData.file && !formData.title) {
      alert("파일을 선택하거나 제목을 입력해주세요.");
      return;
    }

    let fileData = '';
    let fileName = formData.fileName || '파일명.pdf';
    let fileSize = formData.fileSize || '2.4 MB';

    if (formData.file) {
      const reader = new FileReader();
      fileData = await new Promise((resolve) => {
        reader.onload = (e) => resolve(e.target?.result as string);
        reader.readAsDataURL(formData.file!);
      });
      fileName = formData.file.name;
      fileSize = formatFileSize(formData.file.size);
    }

    const newResource: Resource = {
      id: Date.now(),
      title: formData.title || fileName.replace(/\.[^/.]+$/, ""),
      category: formData.category,
      description: formData.description || "자료 설명이 없습니다.",
      fileName: fileName,
      fileSize: fileSize,
      fileType: formData.fileType,
      uploadDate: new Date().toISOString().split('T')[0],
      fileData: fileData || undefined,
    };

    const updated = [newResource, ...resources];
    saveResources(updated);
    
    setShowForm(false);
    setFormData({
      title: '',
      category: '회의록',
      description: '',
      file: null,
      fileName: '',
      fileSize: '',
      fileType: 'pdf',
    });

    alert('자료가 성공적으로 업로드되었습니다!');
  };

  const deleteResource = (id: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    const updated = resources.filter(r => r.id !== id);
    saveResources(updated);
  };

  const downloadResource = (resource: Resource) => {
    if (resource.fileData) {
      const link = document.createElement('a');
      link.href = resource.fileData;
      link.download = resource.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } else {
      alert('다운로드할 파일 데이터가 없습니다. (이전 버전에 업로드된 파일)');
    }
  };

  const getFileIcon = (type: string) => {
    switch (type) {
      case 'pdf': return <FileText className="w-12 h-12 text-red-500" />;
      case 'image': return <Image className="w-12 h-12 text-purple-500" />;
      case 'doc': return <FileText className="w-12 h-12 text-blue-600" />;
      default: return <File className="w-12 h-12 text-gray-500" />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b">
        <div className="max-w-6xl mx-auto px-8 py-5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/dashboard" className="p-2 hover:bg-gray-100 rounded-xl">
              <ArrowLeft className="w-6 h-6" />
            </Link>
            <h1 className="text-3xl font-bold">자료실</h1>
          </div>
          <Button onClick={() => setShowForm(true)} className="rounded-full px-6 py-6 flex items-center gap-2">
            <Plus className="w-5 h-5" />
            자료 업로드
          </Button>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-8 py-10">
        {resources.length === 0 ? (
          <div className="bg-white rounded-3xl py-24 text-center">
            <Upload className="w-16 h-16 mx-auto text-gray-300" />
            <p className="text-xl text-gray-400 mt-6">아직 등록된 자료가 없습니다</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {resources.map((resource) => (
              <div key={resource.id} className="bg-white rounded-3xl p-7 hover:shadow-lg transition group">
                <div className="flex justify-between">
                  <div className="flex gap-5">
                    {getFileIcon(resource.fileType)}
                    <div>
                      <h3 className="font-bold text-lg pr-8 leading-tight">{resource.title}</h3>
                      <p className="text-sm text-gray-500 mt-2">{resource.uploadDate}</p>
                      <p className="text-xs text-gray-400 mt-1">{resource.fileName} • {resource.fileSize}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => deleteResource(resource.id)}
                    className="text-red-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition"
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>

                <div className="mt-5">
                  <span className="bg-blue-100 text-blue-700 text-xs px-4 py-1.5 rounded-2xl">
                    {resource.category}
                  </span>
                </div>

                {resource.description && (
                  <p className="mt-5 text-sm text-gray-600 line-clamp-3">{resource.description}</p>
                )}

                <Button 
                  onClick={() => downloadResource(resource)}
                  className="w-full mt-6 rounded-2xl py-6"
                >
                  <Download className="w-5 h-5 mr-2" />
                  다운로드 하기
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 업로드 모달 */}
      {showForm && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-3xl w-full max-w-lg">
            <div className="p-10">
              <div className="flex justify-between items-center mb-8">
                <h2 className="text-3xl font-bold">자료 업로드</h2>
                <button onClick={() => setShowForm(false)} className="text-gray-400 hover:text-gray-600">
                  <X className="w-6 h-6" />
                </button>
              </div>

              {/* Drag & Drop 영역 */}
              <div
                className={`border-2 border-dashed rounded-3xl p-12 text-center mb-8 transition-colors ${
                  isDragging ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
                }`}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="w-12 h-12 mx-auto text-gray-400" />
                <p className="mt-4 font-medium text-lg">
                  파일을 여기에 끌어다 놓으세요
                </p>
                <p className="text-sm text-gray-500 mt-2">또는 클릭하여 파일 선택</p>
                <input
                  type="file"
                  ref={fileInputRef}
                  className="hidden"
                  onChange={(e) => e.target.files && handleFileSelect(e.target.files[0])}
                />
              </div>

              {formData.fileName && (
                <div className="bg-gray-50 p-4 rounded-2xl mb-6 flex items-center gap-3">
                  <File className="w-8 h-8 text-gray-500" />
                  <div className="text-sm">
                    <p className="font-medium">{formData.fileName}</p>
                    <p className="text-gray-500">{formData.fileSize}</p>
                  </div>
                </div>
              )}

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium mb-2">자료 제목</label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full px-5 py-4 border rounded-2xl"
                    placeholder="자료 제목을 입력하세요"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">카테고리</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full px-5 py-4 border rounded-2xl"
                  >
                    {categories.map(cat => (
                      <option key={cat} value={cat}>{cat}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">설명 (선택)</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full h-24 px-5 py-4 border rounded-3xl resize-y"
                    placeholder="자료에 대한 간단한 설명을 적어주세요"
                  />
                </div>
              </div>

              <div className="flex gap-4 mt-10">
                <Button 
                  variant="outline" 
                  className="flex-1 py-7 rounded-2xl"
                  onClick={() => setShowForm(false)}
                >
                  취소
                </Button>
                <Button 
                  onClick={addResource}
                  className="flex-1 py-7 rounded-2xl"
                >
                  업로드 하기
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}