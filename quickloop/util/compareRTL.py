import filecmp
import os
from anytree import Node, PreOrderIter, find_by_attr
import os.path
from collections import defaultdict

def getSubmodules(moduleName, rtldir):
    filename = f'{rtldir}/{moduleName}.v'
    if not os.path.isfile(filename):
        return []
    ilist = defaultdict(list)
    with open(filename) as f:
        for r in f.readlines():
            tok = [x.strip() for x in r.split()]
            if len(tok) > 3 and '(' == tok[2]:
                ilist[tok[0]].append(tok[1])
    return [(k, len(ilist[k])) for k in ilist.keys()]

def makeTree(module, rtldir):
    submodules = getSubmodules(module.name, rtldir)
    for s, l in submodules:
        x = Node(s, parent = module, ins = l)
        makeTree(x, rtldir)


def getModulehierarchy(moduleName, rtldir):
    root = Node(moduleName)
    makeTree(root, rtldir)
    return root

def getmodulelist(moduleName, rtldir):
    modlist = []
    top = getModulehierarchy(moduleName, rtldir)
    for node in PreOrderIter(top):
        module = node.name
        if not module in modlist:
            modlist.append(module)
    return modlist

rtldir1 = '/work/tayyeb/tmp/dseacc/6/rtl'
rtldir2 = '/work/tayyeb/tmp/dseacc/0/rtl'

def getCommonList(top, rtlDir1, rtlDir2):
    comlist = set(getmodulelist(top, rtlDir1)) & set(getmodulelist(top, rtlDir2))
    diflist = set()
    for f in comlist:
        f1 = f'{rtlDir1}/{f}.v'
        f2 = f'{rtlDir2}/{f}.v'
        if os.path.isfile(f1) and os.path.isfile(f2):
            if not filecmp.cmp(f1, f2):
                diflist.add(f)

    return comlist - diflist

def getCommonListFromDir(top, topDir):
    comlist = set()
    prev = ''
    for dir in os.listdir(topDir):
        rtlDir = f'{topDir}/{dir}/rtl'
        if len(prev) == 0:
            comlist = set(getmodulelist(top, rtlDir))
        else:
            comlist = comlist & getCommonList(top, prev, rtlDir)
        prev = rtlDir
    return comlist



def getBlackboxes(top, rtlDir1, rtlDir2):
    blackboxes = []
    root = getModulehierarchy(top, rtlDir1)
    clist = getCommonList(top, rtlDir1, rtlDir2)

    def checkblacklist(node):
        if node.name not in clist:
            blackboxes.append(node.name)
        else:
            for c in node.children:
                checkblacklist(c)
    checkblacklist(root)
    return blackboxes

def getNumInstances(rtlDir, parent, child):
    n = find_by_attr(getModulehierarchy(parent, rtlDir), child)
    return n.ins
