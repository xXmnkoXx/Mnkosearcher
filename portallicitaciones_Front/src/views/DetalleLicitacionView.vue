<template>
  <div class="page">
    <header class="header">
      <h1>DETALLE DE LA LICITACIÓN</h1>
    </header>

    <section class="card" v-if="loading">
      <div class="state">Cargando…</div>
    </section>

    <section class="card error" v-else-if="error">
      <div class="state error">✖ {{ error }}</div>
    </section>

    <section class="card" v-else-if="d">
      <div class="topbar">
        <div class="title-wrap">
          <div class="expediente" v-if="d.expediente">{{ d.expediente }}</div>
          <h2 class="titulo">{{ d.titulo || '—' }}</h2>
        </div>

        <div class="buttons">
          <button class="btn ghost" type="button">Enviar</button>
          <button class="btn primary" type="button">Descargar</button>
        </div>
      </div>

      <div class="grid">
        <div class="kv">
          <div class="k">EXPEDIENTE</div>
          <div class="v">{{ d.expediente || '-' }}</div>
        </div>

        <div class="kv">
          <div class="k">ORGANISMO</div>
          <div class="v">
            {{ d.organismo || '-' }}
            <div v-if="d.organismoNif" class="sub">NIF: {{ d.organismoNif }}</div>
          </div>
        </div>

        <div class="kv">
          <div class="k">ESTADO</div>
          <div class="v">{{ d.estado || '-' }}</div>
        </div>

        <div class="kv">
          <div class="k">LOCALIZACIÓN</div>
          <div class="v">{{ d.lugarEjecucion || '-' }}</div>
        </div>

        <div class="kv">
          <div class="k">PROCEDIMIENTO</div>
          <div class="v">{{ d.procedimiento || '-' }}</div>
        </div>

        <div class="kv">
          <div class="k">TIPO CONTRATO</div>
          <div class="v">{{ d.tipoContrato || '-' }}</div>
        </div>

        <div class="kv">
          <div class="k">PRESUPUESTO</div>
          <div class="v">{{ formatMoney(d.presupuestoBase, d.moneda) }}</div>
        </div>

        <div class="kv">
          <div class="k">VALOR ESTIMADO</div>
          <div class="v">{{ formatMoney(d.valorEstimado, d.moneda) }}</div>
        </div>

        <div class="kv">
          <div class="k">FECHA PUBLICACIÓN</div>
          <div class="v">{{ formatDate(d.fechaPublicacion) }}</div>
        </div>

        <div class="kv">
          <div class="k">FECHA FIN PRESENTACIÓN OFERTA</div>
          <div class="v">
            <span class="pill" :class="pillClass(d.fechaLimitePresentacion)">
              {{ formatDate(d.fechaLimitePresentacion) }}
            </span>
          </div>
        </div>

        <!-- ✅ EXTRA (si backend los envía). No rompe si vienen null -->
        <div class="kv" v-if="d.modalidad">
          <div class="k">MODALIDAD</div>
          <div class="v">{{ d.modalidad }}</div>
        </div>

        <div class="kv" v-if="d.plazoEjecucion">
          <div class="k">PLAZO EJECUCIÓN</div>
          <div class="v">{{ d.plazoEjecucion }}</div>
        </div>

        <div class="kv col-span-2" v-if="d.motivo">
          <div class="k">MOTIVO</div>
          <div class="v">{{ d.motivo }}</div>
        </div>

        <div class="kv col-span-2" v-if="d.criterioAdjudicacion">
          <div class="k">CRITERIO ADJUDICACIÓN</div>
          <div class="v">{{ d.criterioAdjudicacion }}</div>
        </div>

        <div class="kv col-span-2" v-if="d.fechaUltimaActualizacion">
          <div class="k">ÚLTIMA ACTUALIZACIÓN</div>
          <div class="v">{{ formatDate(d.fechaUltimaActualizacion) }}</div>
        </div>

        <div class="kv col-span-2">
          <div class="k">LINK DE LA LICITACIÓN</div>
          <div class="v">
            <a
              v-if="d.urlPublica"
              :href="d.urlPublica"
              target="_blank"
              rel="noreferrer"
              class="link"
            >
              {{ d.urlPublica }}
            </a>
            <span v-else>-</span>
          </div>
        </div>

        <div class="kv col-span-2" v-if="d.cpvs && d.cpvs.length">
          <div class="k">CPV</div>
          <div class="v">
            <span class="tag" v-for="c in d.cpvs" :key="c">{{ c }}</span>
          </div>
        </div>
      </div>

      <div class="docs" v-if="d.documentos && d.documentos.length">
        <h3>DOCUMENTOS</h3>
        <ul>
          <li v-for="(doc, idx) in d.documentos" :key="doc.url ?? doc.titulo ?? doc.tipo ?? idx">
            <a
              v-if="doc.url"
              :href="doc.url"
              target="_blank"
              rel="noreferrer"
              class="link"
            >
              {{ doc.titulo || doc.tipo || 'Documento' }}
            </a>
            <span v-else>{{ doc.titulo || doc.tipo || 'Documento' }}</span>
          </li>
        </ul>
      </div>

      <div class="back">
        <button class="btn ghost" type="button" @click="goBack">← Volver</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch } from '@/services/api'

type ApiOrganismo = { id?: number; nombre?: string; nif?: string; codigo?: string } | string | null

type ApiDocumento = {
  id?: number
  tipo?: string | null
  titulo?: string | null
  url?: string | null
}

type LicitacionFullApi = {
  idDb?: number
  uuid?: string
  expediente?: string | null
  titulo?: string | null

  organismo?: ApiOrganismo
  organo?: string | null
  entidad?: string | null

  estadoTexto?: string | null
  tipoContratoTexto?: string | null
  procedimiento?: string | null
  lugarEjecucion?: string | null

  fechaPublicacion?: string | null
  fechaLimitePresentacion?: string | null

  precioLicitacion?: number | null
  valorEstimado?: number | null
  moneda?: string | null

  urlPublica?: string | null
  urlDetalle?: string | null

  cpvs?: string[] | null
  documentos?: ApiDocumento[] | null
  lotes?: any[] | null
  adjudicacion?: any | null

  // ✅ campos "extra" por si ya los añades al endpoint del backend
  modalidad?: string | number | null
  motivo?: string | null
  plazoEjecucion?: string | number | null
  criterioAdjudicacion?: string | null
  fechaUltimaActualizacion?: string | null
}

type DetalleVM = {
  expediente?: string | null
  titulo?: string | null

  organismo?: string | null
  organismoNombre?: string | null
  organismoNif?: string | null

  estado?: string | null
  tipoContrato?: string | null
  procedimiento?: string | null
  lugarEjecucion?: string | null

  fechaPublicacion?: string | null
  fechaLimitePresentacion?: string | null

  presupuestoBase?: number | null
  valorEstimado?: number | null
  moneda?: string | null

  urlPublica?: string | null

  cpvs?: string[]
  documentos?: ApiDocumento[]

  // ✅ extras
  modalidad?: string | null
  motivo?: string | null
  plazoEjecucion?: string | null
  criterioAdjudicacion?: string | null
  fechaUltimaActualizacion?: string | null
}

// 🔧 Arreglo “ContrataciÃ³n” -> “Contratación”
function fixUtf8(s?: string | null): string | null {
  if (!s) return s ?? null
  if (!s.includes('Ã') && !s.includes('Â')) return s
  try {
    const bytes = Uint8Array.from(s, (ch) => ch.charCodeAt(0))
    return new TextDecoder('utf-8').decode(bytes)
  } catch {
    return s
  }
}

function normalizeEstado(v?: string | null): string | null {
  if (!v) return null
  const s = v.trim()
  if (s.toLowerCase() === 'pub') return 'Publicada'
  return s
}

function normalizeTipoLicitacion(v?: string | null): string | null {
  if (v === null || v === undefined) return null
  const s = String(v).trim()
  if (s === '1') return 'Abierto'
  if (s === '2') return 'Restringido'
  return s
}

function asText(v: any): string | null {
  if (v === null || v === undefined) return null
  const s = String(v).trim()
  return s ? s : null
}

function mapToVM(raw: LicitacionFullApi): DetalleVM {
  const orgNombre =
    typeof raw.organismo === 'string'
      ? raw.organismo
      : (raw.organismo as any)?.nombre ?? null

  const orgNif =
    typeof raw.organismo === 'string'
      ? null
      : (raw.organismo as any)?.nif ?? null

  const titulo = fixUtf8(raw.titulo ?? null)
  const entidad = fixUtf8(raw.entidad ?? null)
  const organo = fixUtf8(raw.organo ?? null)

  const organismoNombreFix = fixUtf8(orgNombre ?? null)
  const organismo = fixUtf8((organismoNombreFix || organo || entidad || null) as any)

  return {
    expediente: raw.expediente ?? null,
    titulo,

    organismo,
    organismoNombre: organismoNombreFix,
    organismoNif: asText(orgNif),

    estado: normalizeEstado(raw.estadoTexto ?? null),
    procedimiento: normalizeTipoLicitacion(fixUtf8(raw.procedimiento ?? null)),
    tipoContrato: fixUtf8(raw.tipoContratoTexto ?? null),
    lugarEjecucion: fixUtf8(raw.lugarEjecucion ?? null),

    fechaPublicacion: raw.fechaPublicacion ?? null,
    fechaLimitePresentacion: raw.fechaLimitePresentacion ?? null,

    presupuestoBase: raw.precioLicitacion ?? null,
    valorEstimado: raw.valorEstimado ?? null,
    moneda: raw.moneda ?? 'EUR',

    urlPublica: raw.urlPublica || raw.urlDetalle || null,

    cpvs: (raw.cpvs ?? []).filter(Boolean) as string[],
    documentos: (raw.documentos ?? []).map((d) => ({
      ...d,
      tipo: fixUtf8(d.tipo ?? null),
      titulo: fixUtf8(d.titulo ?? null),
      url: d.url ?? null
    })),

    // ✅ extras (si el back los envía)
    modalidad: asText(raw.modalidad),
    motivo: fixUtf8(asText(raw.motivo)),
    plazoEjecucion: asText(raw.plazoEjecucion),
    criterioAdjudicacion: fixUtf8(asText(raw.criterioAdjudicacion)),
    fechaUltimaActualizacion: raw.fechaUltimaActualizacion ?? null
  }
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)
const d = ref<DetalleVM | null>(null)

async function load() {
  loading.value = true
  error.value = null
  d.value = null

  try {
    const id = route.params.id as string
    const data = await apiFetch<LicitacionFullApi>(`/api/licitaciones/${id}`, { method: 'GET' })
    d.value = mapToVM(data)
  } catch (e: any) {
    console.error('[DETALLE] error', e)
    error.value = e?.message || 'No se pudo cargar el detalle'
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(
  () => route.params.id,
  () => load()
)

function goBack() {
  router.push('/mis-licitaciones')
}

function formatDate(iso?: string | null) {
  if (!iso) return '-'
  const dt = new Date(iso)
  if (Number.isNaN(dt.getTime())) return String(iso)
  return dt.toLocaleDateString('es-ES')
}

function formatMoney(amount?: number | null, currency?: string | null) {
  if (amount === null || amount === undefined) return '-'
  const c = currency || 'EUR'
  try {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: c }).format(Number(amount))
  } catch {
    return `${Number(amount).toLocaleString('es-ES')} ${c}`
  }
}

function pillClass(iso?: string | null) {
  if (!iso) return 'pill--neutral'

  const dt = new Date(iso)
  if (Number.isNaN(dt.getTime())) return 'pill--neutral'

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const deadline = new Date(dt)
  deadline.setHours(0, 0, 0, 0)

  const diffMs = deadline.getTime() - today.getTime()
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffDays < 0) return 'pill--red'
  if (diffDays <= 1) return 'pill--yellow'
  return 'pill--green'
}
</script>

<style scoped>
.page {
  padding: 22px;
}

.header h1 {
  margin: 0 0 18px;
  letter-spacing: 0.6px;
  color: #0b1f3b;
}

.card {
  background: #ffffff;
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.18);
}

.state {
  padding: 12px 4px;
  color: #334155;
}

.card.error {
  color: #b91c1c;
}

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.title-wrap {
  min-width: 0;
}

.expediente {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.4px;
  color: #0b1f3b;
  opacity: 0.75;
  margin-bottom: 6px;
}

.titulo {
  margin: 0;
  font-size: 20px;
  color: #0b1f3b;
}

.buttons {
  display: flex;
  gap: 10px;
}

.btn {
  border: 0;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
}

.btn.ghost {
  background: rgba(11, 31, 59, 0.08);
  color: #0b1f3b;
}

.btn.primary {
  background: #0b3a6f;
  color: white;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.kv {
  border: 1px solid rgba(11, 31, 59, 0.12);
  border-radius: 14px;
  padding: 12px;
}

.k {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: rgba(11, 31, 59, 0.6);
  margin-bottom: 6px;
}

.v {
  color: #0b1f3b;
  word-break: break-word;
}

.sub {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 800;
  color: rgba(11, 31, 59, 0.65);
}

.col-span-2 {
  grid-column: span 2;
}

.link {
  color: #0b3a6f;
  text-decoration: underline;
}

.tag {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(11, 31, 59, 0.08);
  margin-right: 6px;
  margin-bottom: 6px;
  font-weight: 700;
  font-size: 12px;
}

.docs {
  margin-top: 16px;
}

.docs h3 {
  margin: 0 0 8px;
  color: #0b1f3b;
}

.docs ul {
  margin: 0;
  padding-left: 18px;
}

.back {
  margin-top: 16px;
}

.pill {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 12px;
  background: rgba(11, 31, 59, 0.08);
  color: #0b1f3b;
}

.pill--neutral {
  opacity: 0.7;
}
.pill--green {
  background: rgba(16, 185, 129, 0.15);
}
.pill--yellow {
  background: rgba(245, 158, 11, 0.18);
}
.pill--red {
  background: rgba(239, 68, 68, 0.18);
}
</style>
