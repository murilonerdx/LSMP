import { useEffect, useRef, useState } from 'react'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { OBJLoader } from 'three/examples/jsm/loaders/OBJLoader.js'

/**
 * Viewer 3D unificado pra modelos do mod. Suporta:
 *  - .bbmodel  (BlockBench JSON nativo)  — parser custom + texturas base64
 *  - .gltf     (GLTF text JSON)          — GLTFLoader do three.js
 *  - .glb      (GLTF binary)             — GLTFLoader.parse(ArrayBuffer)
 *  - .obj      (Wavefront)               — OBJLoader (sem .mtl, sem texturas)
 *
 * Setup compartilhado: scene, camera, lights, grid, orbit controls. Cada formato
 * só muda o passo de "carregar o modelo na cena" — o resto da UI é igual.
 *
 * GLTF text: texturas externas (.png separadas) não funcionam — exporte sempre
 * com texturas embutidas (base64 nas glTF buffers/images). GLB embute tudo em
 * 1 arquivo só — sempre funciona.
 *
 * OBJ: arquivos puros sem .mtl renderizam com material cinza padrão. Sem
 * suporte a texturas via UV (OBJ não tem texturas embutidas).
 */

export type ModelFormat = 'bbmodel' | 'gltf' | 'glb' | 'obj'

export type ModelData =
    | { format: 'bbmodel'; bbmodel: Bbmodel }
    | { format: 'gltf'; json: any }
    | { format: 'glb'; buffer: ArrayBuffer }
    | { format: 'obj'; text: string }

interface BbmodelFace {
    uv: [number, number, number, number]
    texture?: number | null
    rotation?: number
}

interface BbmodelElement {
    name?: string
    uuid?: string
    type?: string
    from: [number, number, number]
    to: [number, number, number]
    origin?: [number, number, number]
    rotation?: [number, number, number]
    uv_offset?: [number, number]
    mirror_uv?: boolean
    visibility?: boolean
    faces?: {
        north?: BbmodelFace
        south?: BbmodelFace
        east?: BbmodelFace
        west?: BbmodelFace
        up?: BbmodelFace
        down?: BbmodelFace
    }
}

interface BbmodelTexture { source: string; name?: string }

/** Entry do outliner: string UUID (= element top-level) OU group object. */
type BbmodelOutlinerEntry = string | BbmodelGroup
interface BbmodelGroup {
    name?: string
    uuid?: string
    origin?: [number, number, number]
    rotation?: [number, number, number]
    visibility?: boolean
    children: BbmodelOutlinerEntry[]
}

export interface Bbmodel {
    name: string
    model_format?: string
    box_uv?: boolean
    resolution?: { width: number; height: number }
    elements: BbmodelElement[]
    textures?: BbmodelTexture[]
    outliner?: BbmodelOutlinerEntry[]
}

interface Props {
    model: ModelData
    width?: number
    height?: number
    autoRotate?: boolean
    background?: string
    showGrid?: boolean
}

export function Model3DViewer({
                                  model,
                                  width = 600,
                                  height = 500,
                                  autoRotate = false,
                                  background = '#1e293b',
                                  showGrid = true,
                              }: Props) {
    const mountRef = useRef<HTMLDivElement>(null)
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!mountRef.current) return
        const mount = mountRef.current
        setError(null)
        setLoading(true)

        let disposed = false
        const disposables: { dispose: () => void }[] = []

        try {
            // ------- Setup compartilhado (scene/camera/lights/renderer/orbit) -------
            const scene = new THREE.Scene()
            scene.background = new THREE.Color(background)

            const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 2000)

            const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
            renderer.setSize(width, height)
            renderer.setPixelRatio(window.devicePixelRatio)
            renderer.outputColorSpace = THREE.SRGBColorSpace
            mount.appendChild(renderer.domElement)
            disposables.push(renderer)

            scene.add(new THREE.AmbientLight(0xffffff, 0.85))
            const dir1 = new THREE.DirectionalLight(0xffffff, 0.6)
            dir1.position.set(1, 1.5, 1)
            scene.add(dir1)
            const dir2 = new THREE.DirectionalLight(0xffffff, 0.4)
            dir2.position.set(-1, 0.5, -1)
            scene.add(dir2)

            // ------- Construir o modelo conforme o formato -------
            const group = new THREE.Group()
            scene.add(group)

            const onModelReady = () => {
                if (disposed) return
                setLoading(false)
                fitCameraToObject(group)
            }

            try {
                if (model.format === 'bbmodel') {
                    buildBbmodel(group, model.bbmodel, disposables)
                    onModelReady()
                } else if (model.format === 'gltf') {
                    const loader = new GLTFLoader()
                    // Texturas externas (URLs) não funcionam — passamos baseURL vazia
                    // pra forçar erro previsível ao invés de tentar carregar de qualquer host.
                    loader.parse(JSON.stringify(model.json), '', (gltf) => {
                        if (disposed) return
                        normalizeImported(gltf.scene)
                        group.add(gltf.scene)
                        onModelReady()
                    }, (e) => {
                        if (disposed) return
                        const msg = (e as any)?.message ?? String(e)
                        setError('GLTF inválido: ' + msg)
                        setLoading(false)
                    })
                } else if (model.format === 'glb') {
                    const loader = new GLTFLoader()
                    loader.parse(model.buffer, '', (gltf) => {
                        if (disposed) return
                        normalizeImported(gltf.scene)
                        group.add(gltf.scene)
                        onModelReady()
                    }, (e) => {
                        if (disposed) return
                        const msg = (e as any)?.message ?? String(e)
                        setError('GLB inválido: ' + msg)
                        setLoading(false)
                    })
                } else if (model.format === 'obj') {
                    const loader = new OBJLoader()
                    const obj = loader.parse(model.text)
                    // OBJ sem .mtl tem materiais default — vamos colorir o objeto pra ficar visível
                    obj.traverse((c) => {
                        if ((c as THREE.Mesh).isMesh) {
                            const mesh = c as THREE.Mesh
                            if (!Array.isArray(mesh.material)) {
                                mesh.material = new THREE.MeshLambertMaterial({
                                    color: 0xa1a1aa, // slate-400
                                    side: THREE.DoubleSide,
                                })
                                disposables.push(mesh.material as THREE.Material)
                            }
                        }
                    })
                    normalizeImported(obj)
                    group.add(obj)
                    onModelReady()
                } else {
                    throw new Error('Formato desconhecido: ' + (model as any).format)
                }
            } catch (loadErr) {
                const msg = loadErr instanceof Error ? loadErr.message : String(loadErr)
                setError(msg)
                setLoading(false)
                return
            }

            // ------- Câmera + orbit (configurado depois que o modelo carregar) -------
            // Valores iniciais — recalculados em fitCameraToObject quando o modelo chega
            let theta = Math.PI / 4
            let phi = Math.PI / 3
            let radius = 32
            let target = new THREE.Vector3(0, 0, 0)

            function fitCameraToObject(obj: THREE.Object3D) {
                const bbox = new THREE.Box3().setFromObject(obj)
                if (bbox.isEmpty()) return
                const size = bbox.getSize(new THREE.Vector3())
                const center = bbox.getCenter(new THREE.Vector3())

                // Centraliza o modelo na origem
                obj.position.sub(center)
                bbox.setFromObject(obj)

                // Grid baseado no tamanho final
                if (showGrid) {
                    const gridSize = Math.max(size.x, size.z, 16) * 1.5
                    const divisions = Math.min(32, Math.max(8, Math.round(gridSize / 2)))
                    const grid = new THREE.GridHelper(gridSize, divisions, 0x64748b, 0x334155)
                    grid.position.y = -size.y / 2 - 0.5
                    scene.add(grid)
                    disposables.push(grid.geometry, grid.material as THREE.Material)
                }

                const maxDim = Math.max(size.x, size.y, size.z, 1)
                radius = maxDim * 2.4
                target.set(0, 0, 0)
                updateCamera()
            }

            function updateCamera() {
                camera.position.set(
                    target.x + radius * Math.sin(phi) * Math.cos(theta),
                    target.y + radius * Math.cos(phi),
                    target.z + radius * Math.sin(phi) * Math.sin(theta)
                )
                camera.lookAt(target)
            }
            updateCamera()

            // ------- Mouse controls -------
            let isDragging = false
            let prevX = 0, prevY = 0

            const onPointerDown = (e: PointerEvent) => {
                isDragging = true
                prevX = e.clientX; prevY = e.clientY
                ;(e.target as HTMLElement).setPointerCapture?.(e.pointerId)
            }
            const onPointerUp = () => { isDragging = false }
            const onPointerMove = (e: PointerEvent) => {
                if (!isDragging) return
                const dx = e.clientX - prevX
                const dy = e.clientY - prevY
                prevX = e.clientX; prevY = e.clientY
                theta -= dx * 0.008
                phi = Math.max(0.05, Math.min(Math.PI - 0.05, phi - dy * 0.008))
                updateCamera()
            }
            const onWheel = (e: WheelEvent) => {
                e.preventDefault()
                const minR = 0.5
                const maxR = 1000
                radius = Math.max(minR, Math.min(maxR, radius + e.deltaY * radius * 0.001))
                updateCamera()
            }

            renderer.domElement.style.cursor = 'grab'
            renderer.domElement.style.touchAction = 'none'
            renderer.domElement.addEventListener('pointerdown', onPointerDown)
            renderer.domElement.addEventListener('pointerup', onPointerUp)
            renderer.domElement.addEventListener('pointermove', onPointerMove)
            renderer.domElement.addEventListener('wheel', onWheel, { passive: false })

            // ------- Animation loop -------
            let raf = 0
            const tick = () => {
                if (disposed) return
                if (autoRotate && !isDragging) {
                    theta += 0.004
                    updateCamera()
                }
                renderer.render(scene, camera)
                raf = requestAnimationFrame(tick)
            }
            tick()

            return () => {
                disposed = true
                cancelAnimationFrame(raf)
                renderer.domElement.removeEventListener('pointerdown', onPointerDown)
                renderer.domElement.removeEventListener('pointerup', onPointerUp)
                renderer.domElement.removeEventListener('pointermove', onPointerMove)
                renderer.domElement.removeEventListener('wheel', onWheel)
                disposables.forEach(d => {
                    try { d.dispose() } catch {}
                })
                if (mount.contains(renderer.domElement)) {
                    mount.removeChild(renderer.domElement)
                }
            }
        } catch (err) {
            const msg = err instanceof Error ? err.message : String(err)
            setError(msg)
            setLoading(false)
            return
        }
    }, [model, width, height, autoRotate, background, showGrid])

    if (error) {
        return (
            <div
                style={{ width, height }}
                className="flex flex-col items-center justify-center rounded-lg border border-red-700 bg-red-950/30 p-4 text-center text-sm text-red-200"
            >
                <div className="mb-2 text-2xl">⚠️</div>
                <div className="font-semibold">Erro ao renderizar modelo</div>
                <div className="mt-1 text-xs opacity-80">{error}</div>
            </div>
        )
    }

    return (
        <div className="relative">
            <div
                ref={mountRef}
                style={{ width, height }}
                className="overflow-hidden rounded-lg border border-slate-700 bg-slate-900"
            />
            {loading && (
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                    <div className="bg-slate-800/80 px-3 py-1.5 rounded text-xs text-slate-300 animate-pulse">
                        carregando modelo...
                    </div>
                </div>
            )}
        </div>
    )
}

// ============================================================
// Helpers
// ============================================================

/**
 * Modelos importados (GLTF/OBJ) podem ter escala muito grande ou muito pequena.
 * Normaliza pra caber no viewport — escala uniforme baseada na maior dimensão.
 */
function normalizeImported(obj: THREE.Object3D) {
    const bbox = new THREE.Box3().setFromObject(obj)
    if (bbox.isEmpty()) return
    const size = bbox.getSize(new THREE.Vector3())
    const max = Math.max(size.x, size.y, size.z)
    if (max <= 0) return
    // Escala pra ~16 unidades (similar ao tamanho de 1 bloco do MC)
    if (max < 0.5 || max > 64) {
        const s = 16 / max
        obj.scale.multiplyScalar(s)
    }
}

/**
 * Constrói geometria a partir de um .bbmodel parsed.
 *
 * <p>v0.1.12+: processa o {@code outliner} corretamente — modelos do BlockBench
 * agrupam elements em hierarquia de groups, cada um com rotation/origin própria
 * que se aplica aos children. Antes a gente ignorava isso → cubos apareciam
 * em posições/rotações erradas pros models complexos.
 *
 * <p>Hierarquia: outliner[] → cada entry é string (UUID de element) ou object
 * (group com {name, origin, rotation, children}). Recursivo.
 */
function buildBbmodel(parent: THREE.Group, bb: Bbmodel, disposables: { dispose: () => void }[]) {
    let texture: THREE.Texture | null = null
    if (bb.textures && bb.textures.length > 0 && bb.textures[0].source) {
        const img = new Image()
        img.src = bb.textures[0].source
        texture = new THREE.Texture(img)
        texture.magFilter = THREE.NearestFilter
        texture.minFilter = THREE.NearestFilter
        texture.flipY = false
        texture.colorSpace = THREE.SRGBColorSpace
        img.onload = () => { if (texture) texture.needsUpdate = true }
        disposables.push(texture)
    }

    const visibleMat = new THREE.MeshLambertMaterial({
        map: texture,
        transparent: true,
        alphaTest: 0.05,
        side: THREE.DoubleSide,
        color: texture ? 0xffffff : 0xaaaaaa,
    })
    const hiddenMat = new THREE.MeshBasicMaterial({ visible: false })
    disposables.push(visibleMat, hiddenMat)

    const resW = bb.resolution?.width ?? 16
    const resH = bb.resolution?.height ?? 16
    const boxUv = bb.box_uv ?? false

    const faceMap: Array<{ bbFace: keyof NonNullable<BbmodelElement['faces']> }> = [
        { bbFace: 'east' }, { bbFace: 'west' }, { bbFace: 'up' },
        { bbFace: 'down' }, { bbFace: 'south' }, { bbFace: 'north' },
    ]

    // Função interna que cria o Mesh pra um element (sem aplicar group rotation
    // ainda — isso fica pra outliner pass).
    const elementMeshes = new Map<string, THREE.Mesh>()
    const orphanMeshes: THREE.Mesh[] = [] // meshes sem UUID — adiciona direto

    function buildElementMesh(el: BbmodelElement): THREE.Mesh | null {
        if (el.type && el.type !== 'cube') return null
        if (el.visibility === false) return null
        if (!el.from || !el.to) return null

        const [fx, fy, fz] = el.from
        const [tx, ty, tz] = el.to
        const w = tx - fx, h = ty - fy, d = tz - fz
        if (w === 0 || h === 0 || d === 0) return null

        const geom = new THREE.BoxGeometry(Math.abs(w), Math.abs(h), Math.abs(d))
        disposables.push(geom)

        const materials: THREE.Material[] = []
        const uvAttr = geom.attributes.uv as THREE.BufferAttribute
        const uvArr = uvAttr.array as Float32Array

        faceMap.forEach((fm, faceIdx) => {
            let u1 = 0, v1 = 0, u2 = 0, v2 = 0
            let hidden = false

            if (boxUv && el.uv_offset) {
                const [uo, vo] = el.uv_offset
                switch (fm.bbFace) {
                    case 'east':  u1 = uo + d + w;     v1 = vo + d; u2 = uo + d + w + d;     v2 = vo + d + h; break
                    case 'west':  u1 = uo;             v1 = vo + d; u2 = uo + d;             v2 = vo + d + h; break
                    case 'up':    u1 = uo + d;         v1 = vo;     u2 = uo + d + w;         v2 = vo + d; break
                    case 'down':  u1 = uo + d + w;     v1 = vo;     u2 = uo + d + w + w;     v2 = vo + d; break
                    case 'south': u1 = uo + d + w + d; v1 = vo + d; u2 = uo + d + w + d + w; v2 = vo + d + h; break
                    case 'north': u1 = uo + d;         v1 = vo + d; u2 = uo + d + w;         v2 = vo + d + h; break
                }
            } else if (el.faces && el.faces[fm.bbFace]) {
                const f = el.faces[fm.bbFace]!
                if (!f.uv || f.texture === null || f.texture === undefined) {
                    hidden = true
                } else {
                    u1 = f.uv[0]; v1 = f.uv[1]; u2 = f.uv[2]; v2 = f.uv[3]
                }
            } else {
                hidden = true
            }

            materials.push(hidden ? hiddenMat : visibleMat)

            const uMin = u1 / resW, uMax = u2 / resW
            const vMin = v1 / resH, vMax = v2 / resH
            const base = faceIdx * 8
            uvArr[base + 0] = uMin; uvArr[base + 1] = vMin
            uvArr[base + 2] = uMax; uvArr[base + 3] = vMin
            uvArr[base + 4] = uMin; uvArr[base + 5] = vMax
            uvArr[base + 6] = uMax; uvArr[base + 7] = vMax
        })
        uvAttr.needsUpdate = true

        const mesh = new THREE.Mesh(geom, materials)
        mesh.position.set((fx + tx) / 2, (fy + ty) / 2, (fz + tz) / 2)

        if (el.rotation && el.origin && (el.rotation[0] || el.rotation[1] || el.rotation[2])) {
            const [rx, ry, rz] = el.rotation
            const [ox, oy, oz] = el.origin
            const pivot = new THREE.Vector3(ox, oy, oz)
            mesh.position.sub(pivot)
            const euler = new THREE.Euler(
                THREE.MathUtils.degToRad(rx),
                THREE.MathUtils.degToRad(ry),
                THREE.MathUtils.degToRad(rz),
                'ZYX'
            )
            mesh.position.applyEuler(euler)
            mesh.position.add(pivot)
            mesh.setRotationFromEuler(euler)
        }

        return mesh
    }

    // PASS 1: cria os meshes de cada element e guarda por UUID
    for (const el of bb.elements || []) {
        const mesh = buildElementMesh(el)
        if (!mesh) continue
        if (el.uuid) elementMeshes.set(el.uuid, mesh)
        else orphanMeshes.push(mesh)
    }

    // PASS 2: monta hierarquia via outliner. Cada GROUP vira um THREE.Group
    // com origin + rotation aplicados, e contém os meshes children.
    function processOutlinerEntry(entry: BbmodelOutlinerEntry, target: THREE.Group) {
        if (typeof entry === 'string') {
            // É um UUID de element direto
            const m = elementMeshes.get(entry)
            if (m) {
                target.add(m)
                elementMeshes.delete(entry) // marca como usado
            }
            return
        }
        // É um group object
        if (entry.visibility === false) return
        const groupNode = new THREE.Group()
        groupNode.name = entry.name ?? 'group'

        const origin = entry.origin ?? [0, 0, 0]
        const rotation = entry.rotation ?? [0, 0, 0]
        const hasRotation = rotation[0] || rotation[1] || rotation[2]

        // Processa children primeiro (sem aplicar rotation do group)
        for (const child of entry.children ?? []) {
            processOutlinerEntry(child, groupNode)
        }

        // Aplica rotation/origin do GROUP via pivot
        if (hasRotation) {
            // Translate children pra serem relativos ao pivot
            groupNode.position.set(origin[0], origin[1], origin[2])
            groupNode.rotation.set(
                THREE.MathUtils.degToRad(rotation[0]),
                THREE.MathUtils.degToRad(rotation[1]),
                THREE.MathUtils.degToRad(rotation[2]),
                'ZYX'
            )
            // Children precisam estar em coord local do pivot
            groupNode.children.forEach(c => {
                c.position.sub(new THREE.Vector3(origin[0], origin[1], origin[2]))
            })
        }

        target.add(groupNode)
    }

    if (bb.outliner && bb.outliner.length > 0) {
        for (const entry of bb.outliner) {
            processOutlinerEntry(entry, parent)
        }
    }

    // Elements sem UUID OU não referenciados pelo outliner — adiciona direto
    elementMeshes.forEach(m => parent.add(m))
    orphanMeshes.forEach(m => parent.add(m))

    if (parent.children.length === 0) {
        throw new Error('Modelo .bbmodel não tem elementos renderizáveis (apenas meshes/polígonos não suportados).')
    }
}

// ============================================================
// Loader unificado (usado pelos modais que fazem fetch)
// ============================================================

/**
 * Faz fetch autenticado de uma URL e converte pra ModelData baseado no formato.
 * Centraliza a lógica de fetch+parse pra modais não duplicarem código.
 */
export async function fetchModelData(streamUrl: string, format: ModelFormat, token: string): Promise<ModelData> {
    const r = await fetch(streamUrl, {
        headers: { Authorization: `Bearer ${token}` },
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}`)

    if (format === 'bbmodel') {
        const json = await r.json()
        if (!json.elements) throw new Error('arquivo não é um .bbmodel válido')
        return { format, bbmodel: json }
    }
    if (format === 'gltf') {
        const json = await r.json()
        if (!json.asset && !json.meshes && !json.nodes) throw new Error('arquivo não é um .gltf válido')
        return { format, json }
    }
    if (format === 'glb') {
        const buffer = await r.arrayBuffer()
        if (buffer.byteLength < 12) throw new Error('arquivo .glb truncado')
        return { format, buffer }
    }
    if (format === 'obj') {
        const text = await r.text()
        if (!text.includes('v ') && !text.includes('f ')) throw new Error('arquivo .obj sem vertices/faces')
        return { format, text }
    }
    throw new Error('formato desconhecido: ' + format)
}
