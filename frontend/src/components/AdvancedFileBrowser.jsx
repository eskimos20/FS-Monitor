import React, { useState, useEffect, useCallback } from 'react';
import { 
  Dialog, 
  DialogBackdrop, 
  DialogPanel, 
  DialogTitle,
  Transition,
  TransitionChild
} from '@headlessui/react';
import { 
  Folder, 
  FolderOpen, 
  File, 
  HardDrive, 
  ChevronRight, 
  ChevronDown, 
  ArrowLeft, 
  Check,
  X,
  Loader2,
  Home,
  FolderTree
} from 'lucide-react';

const AdvancedFileBrowser = ({ onPathSelect, initialPath = '/', isOpen, onClose, allowFileSelection = false }) => {
  const [currentPath, setCurrentPath] = useState(initialPath);
  const [treeData, setTreeData] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [selectedPath, setSelectedPath] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [roots, setRoots] = useState([]);
  const [showRoots, setShowRoots] = useState(true);

  // Load root directories
  const loadRoots = useCallback(async () => {
    try {
      const response = await fetch('/api/file-browser/roots');
      const data = await response.json();
      
      if (response.ok) {
        const rootsData = data.map(root => ({
          ...root,
          id: root.path,
          name: root.name,
          type: 'root',
          children: [],
          isExpanded: false
        }));
        setRoots(rootsData);
      } else {
        setError('Failed to load root directories');
      }
    } catch (err) {
      setError('Failed to load root directories');
    }
  }, []);

  // Load directory contents
  const loadDirectory = useCallback(async (path, nodeId) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/api/file-browser/list?path=${encodeURIComponent(path)}`);
      const data = await response.json();
      
      if (response.ok) {
        const directories = (data.directories || []).map(dir => ({
          ...dir,
          id: dir.path,
          type: 'directory',
          children: [],
          isExpanded: false,
          parentPath: path
        }));
        
        const files = (data.files || []).map(file => ({
          ...file,
          id: file.path,
          type: 'file',
          parentPath: path
        }));
        
        // Update tree data
        if (showRoots) {
          setTreeData(directories);
        } else {
          const updatedTree = updateNodeInTree(treeData, nodeId, { children: [...directories, ...files] });
          setTreeData(updatedTree);
        }
        
        return { directories, files };
      } else {
        setError(data.error || 'Failed to load directory');
        return { directories: [], files: [] };
      }
    } catch (err) {
      setError('Failed to connect to server');
      return { directories: [], files: [] };
    } finally {
      setLoading(false);
    }
  }, [showRoots, treeData]);

  // Update a specific node in the tree
  const updateNodeInTree = (tree, nodeId, updates) => {
    const updateTree = (nodes) => {
      return nodes.map(node => {
        if (node.id === nodeId) {
          return { ...node, ...updates };
        }
        if (node.children && node.children.length > 0) {
          return {
            ...node,
            children: updateTree(node.children)
          };
        }
        return node;
      });
    };
    
    return updateTree(tree);
  };

  // Toggle node expansion
  const toggleNode = useCallback(async (node) => {
    if (node.type === 'file') return;
    
    const isExpanded = expandedNodes.has(node.id);
    const newExpanded = new Set(expandedNodes);
    
    if (isExpanded) {
      newExpanded.delete(node.id);
      setExpandedNodes(newExpanded);
    } else {
      newExpanded.add(node.id);
      setExpandedNodes(newExpanded);
      
      // Load children if not already loaded
      if (!node.children || node.children.length === 0) {
        await loadDirectory(node.path, node.id);
      }
    }
  }, [expandedNodes, loadDirectory]);

  // Handle path selection
  const handleNodeClick = useCallback((node) => {
    // Allow file selection if allowFileSelection is true
    if (node.type === 'file' && allowFileSelection) {
      setSelectedPath(node.path);
      return;
    }
    
    if (node.type === 'directory' || node.type === 'root') {
      setSelectedPath(node.path);
      
      // Switch to tree view when clicking on a root directory
      if (node.type === 'root' && showRoots) {
        setShowRoots(false);
        setCurrentPath(node.path);
        // Load the directory content as tree data
        loadDirectory(node.path, node.id);
      } else {
        toggleNode(node);
      }
    }
  }, [showRoots, toggleNode, loadDirectory, allowFileSelection]);

  // Handle double click for selection
  const handleNodeDoubleClick = useCallback((node) => {
    if (node.type === 'directory') {
      setSelectedPath(node.path);
    }
  }, []);

  // Confirm selection
  const confirmSelection = useCallback(() => {
    if (selectedPath) {
      onPathSelect(selectedPath);
      onClose();
    }
  }, [selectedPath, onPathSelect, onClose]);

  // Navigate to parent
  const navigateToParent = useCallback(() => {
    if (currentPath !== '/') {
      const parentPath = currentPath.split('/').slice(0, -1).join('/') || '/';
      setCurrentPath(parentPath);
      setShowRoots(false);
    } else {
      setShowRoots(true);
    }
  }, [currentPath]);

  // Navigate to root
  const navigateToRoot = useCallback(() => {
    setShowRoots(true);
    setCurrentPath('/');
    setSelectedPath(null);
  }, []);

  // Initialize
  useEffect(() => {
    if (isOpen) {
      loadRoots();
      if (initialPath && initialPath !== '/') {
        setCurrentPath(initialPath);
        setSelectedPath(initialPath);
        setShowRoots(false);
      }
    }
  }, [isOpen, initialPath, loadRoots]);

  // Render tree node
  const renderNode = (node, level = 0) => {
    const isExpanded = expandedNodes.has(node.id);
    const isSelected = selectedPath === node.path;
    const hasChildren = node.type === 'directory';
    const isLoading = loading && expandedNodes.has(node.id) && (!node.children || node.children.length === 0);

    return (
      <div key={node.id}>
        <div
          className={`
            flex items-center py-2 px-3 cursor-pointer hover:bg-gray-50 rounded-lg transition-colors
            ${isSelected ? 'bg-blue-50 border border-blue-200' : ''}
            ${level > 0 ? `ml-${Math.min(level * 4, 12)}` : ''}
          `}
          style={{ paddingLeft: `${level * 20 + 12}px` }}
          onClick={() => handleNodeClick(node)}
          onDoubleClick={() => handleNodeDoubleClick(node)}
        >
          {hasChildren && (
            <div className="mr-1">
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
              ) : isExpanded ? (
                <ChevronDown className="h-4 w-4 text-gray-400" />
              ) : (
                <ChevronRight className="h-4 w-4 text-gray-400" />
              )}
            </div>
          )}
          
          <div className="mr-3">
            {node.type === 'root' && <HardDrive className="h-5 w-5 text-blue-500" />}
            {node.type === 'directory' && (
              isExpanded ? (
                <FolderOpen className="h-5 w-5 text-blue-500" />
              ) : (
                <Folder className="h-5 w-5 text-blue-500" />
              )
            )}
            {node.type === 'file' && <File className="h-5 w-5 text-gray-400" />}
          </div>
          
          <div className="flex-1 min-w-0">
            <div className={`text-sm font-medium truncate ${isSelected ? 'text-blue-900' : 'text-gray-900'}`}>
              {node.name}
            </div>
            {node.type === 'file' && node.size && (
              <div className="text-xs text-gray-500">
                {formatFileSize(node.size)}
              </div>
            )}
          </div>
          
          {isSelected && (
            <Check className="h-4 w-4 text-blue-600 ml-2" />
          )}
        </div>
        
        {hasChildren && isExpanded && node.children && node.children.length > 0 && (
          <div>
            {node.children.map(child => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    );
  };

  // Format file size
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  return (
    <Transition appear show={isOpen} as={React.Fragment}>
      <Dialog as="div" className="relative z-50" onClose={onClose}>
        <DialogBackdrop className="fixed inset-0 bg-black bg-opacity-25 transition-opacity" />
        
        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex min-h-full items-center justify-center p-4">
            <TransitionChild
              enter="ease-out duration-300"
              enterFrom="opacity-0 scale-95"
              enterTo="opacity-100 scale-100"
              leave="ease-in duration-200"
              leaveFrom="opacity-100 scale-100"
              leaveTo="opacity-0 scale-95"
            >
              <DialogPanel className="w-full max-w-4xl transform overflow-hidden rounded-2xl bg-white p-6 text-left align-middle shadow-xl transition-all">
                <DialogTitle className="text-lg font-semibold text-gray-900 mb-4">
                  Select Directory
                </DialogTitle>
                
                {/* Breadcrumb */}
                <div className="flex items-center space-x-2 mb-4 text-sm text-gray-600">
                  <button
                    onClick={navigateToRoot}
                    className="hover:text-blue-600 flex items-center"
                  >
                    <Home className="h-4 w-4 mr-1" />
                    Root
                  </button>
                  {!showRoots && currentPath.split('/').filter(Boolean).map((part, index, array) => {
                    const path = '/' + array.slice(0, index + 1).join('/');
                    return (
                      <React.Fragment key={path}>
                        <span>/</span>
                        <button
                          onClick={() => setCurrentPath(path)}
                          className="hover:text-blue-600"
                        >
                          {part}
                        </button>
                      </React.Fragment>
                    );
                  })}
                </div>

                {/* Toolbar */}
                <div className="flex items-center justify-between mb-4 p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={navigateToParent}
                      disabled={showRoots}
                      className="flex items-center px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <ArrowLeft className="h-4 w-4 mr-1" />
                      Up
                    </button>
                    <button
                      onClick={() => loadDirectory(currentPath, 'root')}
                      className="flex items-center px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50"
                    >
                      Refresh
                    </button>
                  </div>
                  
                  {selectedPath && (
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-gray-600">Selected:</span>
                      <span className="text-sm font-medium text-blue-600 truncate max-w-xs">
                        {selectedPath}
                      </span>
                    </div>
                  )}
                </div>

                {/* Error message */}
                {error && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg">
                    {error}
                  </div>
                )}

                {/* Tree View */}
                <div className="border border-gray-200 rounded-lg p-4 bg-gray-50 max-h-96 overflow-y-auto">
                  {showRoots ? (
                    <div className="space-y-1">
                      <div className="flex items-center mb-3 text-sm font-medium text-gray-700">
                        <FolderTree className="h-4 w-4 mr-2" />
                        Root Directories
                      </div>
                      {roots.map(root => renderNode(root))}
                    </div>
                  ) : (
                    <div className="space-y-1">
                      {treeData.length === 0 && !loading && (
                        <div className="text-center py-8 text-gray-500">
                          <Folder className="h-12 w-12 mx-auto mb-2 text-gray-300" />
                          <p>Empty directory</p>
                        </div>
                      )}
                      {treeData.map(node => renderNode(node))}
                    </div>
                  )}
                </div>

                {/* Action buttons */}
                <div className="mt-4 flex justify-end gap-3">
                  <button
                    onClick={confirmSelection}
                    disabled={!selectedPath}
                    className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Select
                  </button>
                  <button
                    onClick={onClose}
                    className="btn-secondary"
                  >
                    Cancel
                  </button>
                </div>
              </DialogPanel>
            </TransitionChild>
          </div>
        </div>
      </Dialog>
    </Transition>
  );
};

export default AdvancedFileBrowser;
