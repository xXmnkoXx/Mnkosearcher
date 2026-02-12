<template>
  <div class="page">
    <header class="header">
      <div class="hstack">
        <h1>BUSCADOR</h1>
        <span class="total-chip">TOTAL: {{ formatCount(totalAll) }}</span>
      </div>
    </header>

    <section class="card">
      <!-- TABS -->
      <div class="tabs">
        <button
          class="tab"
          :class="{ active: store.tab === 'EN_PLAZO' }"
          type="button"
          @click="store.setTab('EN_PLAZO')"
        >
          <span class="tab-ico">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M7 2a1 1 0 0 1 1 1v1h8V3a1 1 0 1 1 2 0v1h1.5A2.5 2.5 0 0 1 22 6.5v13A2.5 2.5 0 0 1 19.5 22h-15A2.5 2.5 0 0 1 2 19.5v-13A2.5 2.5 0 0 1 4.5 4H6V3a1 1 0 0 1 1-1Zm13 8H4v9.5c0 .276.224.5.5.5h15c.276 0 .5-.224.5-.5V10ZM4.5 6a.5.5 0 0 0-.5.5V8h16V6.5a.5.5 0 0 0-.5-.5H18v1a1 1 0 1 1-2 0V6H8v1a1 1 0 1 1-2 0V6H4.5Z"
              />
            </svg>
          </span>
          <span class="tab-text">EN PLAZO ({{ formatCount(counts.EN_PLAZO) }})</span>
        </button>

        <button
          class="tab"
          :class="{ active: store.tab === 'FAVORITAS' }"
          type="button"
          @click="store.setTab('FAVORITAS')"
        >
          <span class="tab-ico">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M14.7 2.3a1 1 0 0 1 1.4 0l5.6 5.6a1 1 0 0 1 0 1.4l-3.2 3.2.8 4.5a1 1 0 0 1-1.2 1.2l-4.5-.8-7.2 7.2a1 1 0 0 1-1.4-1.4l7.2-7.2-.8-4.5a1 1 0 0 1 1.2-1.2l4.5.8 3.2-3.2-4.2-4.2-3.2 3.2a1 1 0 0 1-1.4-1.4l3.4-3.4Z"
              />
            </svg>
          </span>
          <span class="tab-text">FAVORITAS ({{ formatCount(counts.FAVORITAS) }})</span>
        </button>

        <button
          class="tab"
          :class="{ active: store.tab === 'DESCARTADAS' }"
          type="button"
          @click="store.setTab('DESCARTADAS')"
        >
          <span class="tab-ico">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M9 3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1h5a1 1 0 1 1 0 2h-1v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6H4a1 1 0 1 1 0-2h5V3Zm2 1v0h2V4h-2ZM7 6v14h10V6H7Zm3 3a1 1 0 0 1 1 1v7a1 1 0 1 1-2 0v-7a1 1 0 0 1 1-1Zm5 1v7a1 1 0 1 1-2 0v-7a1 1 0 1 1 2 0Z"
              />
            </svg>
          </span>
          <span class="tab-text">DESCARTADAS ({{ formatCount(counts.DESCARTADAS) }})</span>
        </button>

        <button
          class="tab"
          :class="{ active: store.tab === 'VENCIDAS' }"
          type="button"
          @click="store.setTab('VENCIDAS')"
        >
          <span class="tab-ico">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M7 2a1 1 0 0 1 1 1v1h8V3a1 1 0 1 1 2 0v1h1.5A2.5 2.5 0 0 1 22 6.5v13A2.5 2.5 0 0 1 19.5 22h-15A2.5 2.5 0 0 1 2 19.5v-13A2.5 2.5 0 0 1 4.5 4H6V3a1 1 0 0 1 1-1Zm13 8H4v9.5c0 .276.224.5.5.5h15c.276 0 .5-.224.5-.5V10ZM4.5 6a.5.5 0 0 0-.5.5V8h16V6.5a.5.5 0 0 0-.5-.5H18v1a1 1 0 1 1-2 0V6H8v1a1 1 0 1 1-2 0V6H4.5Z"
              />
              <path
                d="M9.7 12.7a1 1 0 0 1 1.4 0L12 13.6l.9-.9a1 1 0 1 1 1.4 1.4l-.9.9.9.9a1 1 0 1 1-1.4 1.4l-.9-.9-.9.9a1 1 0 1 1-1.4-1.4l.9-.9-.9-.9a1 1 0 0 1 0-1.4Z"
              />
            </svg>
          </span>
          <span class="tab-text">VENCIDAS ({{ formatCount(counts.VENCIDAS) }})</span>
        </button>
      </div>

      <!-- TOOLBAR -->
      <div class="toolbar">
        <div class="search">
          <span class="search-ico" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path
                d="M10.5 3a7.5 7.5 0 1 1 4.62 13.4l4.24 4.24a1 1 0 0 1-1.41 1.41l-4.24-4.24A7.5 7.5 0 0 1 10.5 3Zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11Z"
              />
            </svg>
          </span>

          <input
            :value="store.q"
            @input="onQuery(($event.target as HTMLInputElement).value)"
            placeholder="Buscar por objeto, organismo..."
          />
        </div>

        <div class="actions">
          <button class="btn ghost" type="button" @click="openFilters">
            <span class="btn-ico" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path
                  d="M3 5a1 1 0 0 1 1-1h16a1 1 0 0 1 .8 1.6l-6.8 9.06V20a1 1 0 0 1-1.45.9l-3-1.5A1 1 0 0 1 9 18.5v-3.84L3.2 5.6A1 1 0 0 1 3 5Zm3 1 5.2 6.93a1 1 0 0 1 .2.6v3.35l1 .5v-3.85a1 1 0 0 1 .2-.6L18 6H6Z"
                />
              </svg>
            </span>
            Filtrar
          </button>

          <button class="btn primary" type="button" @click="downloadCsv">
            <span class="btn-ico" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path
                  d="M7 2h7l5 5v15a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm7 1.5V8h4.5L14 3.5ZM7 10a1 1 0 0 1 1-1h8a1 1 0 1 1 0 2H8a1 1 0 0 1-1-1Zm0 4a1 1 0 0 1 1-1h8a1 1 0 1 1 0 2H8a1 1 0 0 1-1-1Zm0 4a1 1 0 0 1 1-1h5a1 1 0 1 1 0 2H8a1 1 0 0 1-1-1Z"
                />
              </svg>
            </span>
            Descargar
          </button>
        </div>
      </div>

      <div v-if="store.loading" class="state">Cargando…</div>
      <div v-else-if="store.error" class="state error">✖ {{ store.error }}</div>

      <!-- TABLE + PAGINATION -->
      <div v-else class="table-area">
        <div class="table-wrap" role="region" aria-label="Tabla de licitaciones">
          <table class="table">
            <thead>
              <tr>
                <th class="col-interes">INTERÉS</th>
                <th class="col-exp">EXPEDIENTE Y OBJETO DEL CONTRATO</th>
                <th class="col-org">ORGANISMO</th>
                <th class="col-fecha">
                  F. LÍMITE
                  <span class="sort" aria-hidden="true">↑</span>
                </th>
                <th class="col-importe">
                  IMPORTE
                  <span class="sort" aria-hidden="true">↑</span>
                </th>
                <th class="col-prov">PROVINCIA</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="row in rows" :key="row.id">
                <td class="col-interes">
                  <button
                    class="icon-circle"
                    type="button"
                    title="Descartar"
                    aria-label="Descartar"
                    :aria-pressed="(row as any).__descartada ? 'true' : 'false'"
                    :class="{ 'is-active': (row as any).__descartada }"
                    :disabled="(row as any).__vencida"
                    @click.stop="store.toggleDescartada(row)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        d="M9 3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1h5a1 1 0 1 1 0 2h-1v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6H4a1 1 0 1 1 0-2h5V3Zm2 1v0h2V4h-2ZM7 6v14h10V6H7Z"
                      />
                    </svg>
                  </button>

                  <button
                    class="icon-circle"
                    type="button"
                    title="Favorita"
                    aria-label="Favorita"
                    :aria-pressed="(row as any).__favorita ? 'true' : 'false'"
                    :class="{ 'is-active': (row as any).__favorita }"
                    :disabled="(row as any).__vencida"
                    @click.stop="store.toggleFavorita(row)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        d="M12 2.8l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.9 6.1 20.8l1.2-6.5-4.8-4.6 6.6-.9 2.9-6Zm0 4.4-1.8 3.8-.4.8-.9.1-4.2.6 3.1 3 .7.7-.2.9-.8 4.1 3.8-2 .8-.4.8.4 3.8 2-.8-4.1-.2-.9.7-.7 3.1-3-4.2-.6-.9-.1-.4-.8L12 7.2Z"
                      />
                    </svg>
                  </button>
                </td>

                <td class="col-exp">
                  <div class="exp-badge">{{ row.expediente }}</div>
                  <a class="link" href="#" @click.prevent="goDetalle(row)">
                    {{ row.titulo }}
                  </a>
                </td>

                <td class="col-org">
                  <div class="org">{{ displayOrganismo(row) }}</div>
                </td>

                <td class="col-fecha">
                  <span class="date-pill" :class="pillClass(row)">{{ formatFechaLimite(row) }}</span>
                </td>

                <td class="col-importe">
                  {{ formatMoney(row.presupuestoBase ?? row.valorEstimado, row.moneda) }}
                </td>

                <td class="col-prov">
                  {{ formatProvincia(row.lugarEjecucion) }}
                </td>
              </tr>

              <tr v-if="rows.length === 0">
                <td colspan="6" class="empty">No hay resultados.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pager">
          <div class="pager-info">
            Mostrando
            <strong>{{ pageFrom }}</strong>–<strong>{{ pageTo }}</strong>
            de <strong>{{ totalAll }}</strong>
          </div>

          <div class="pager-actions">
            <button class="pbtn" type="button" :disabled="store.page === 0" @click="store.prevPage()">
              Anterior
            </button>

            <div class="page-pill">
              Página <strong>{{ page }}</strong> / <strong>{{ totalPages }}</strong>
            </div>

            <button
              class="pbtn"
              type="button"
              :disabled="store.page + 1 >= store.totalPages"
              @click="store.nextPage()"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>

      <!-- ✅ MODAL FILTROS -->
      <div v-if="filtersOpen" class="modal-backdrop" role="dialog" aria-modal="true" aria-label="Filtros">
        <div class="modal" @click.stop>
          <div class="modal-head">
            <div class="modal-title">Filtros</div>
            <button class="icon-x" type="button" aria-label="Cerrar" @click="closeFilters">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M18.3 5.7a1 1 0 0 1 0 1.4L13.4 12l4.9 4.9a1 1 0 1 1-1.4 1.4L12 13.4l-4.9 4.9a1 1 0 0 1-1.4-1.4l4.9-4.9-4.9-4.9a1 1 0 0 1 1.4-1.4l4.9 4.9 4.9-4.9a1 1 0 0 1 1.4 0Z"
                />
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="grid2">
              <div class="field">
                <label>Fecha límite desde</label>
                <input type="date" v-model="draftFilters.fechaDesde" />
              </div>
              <div class="field">
                <label>Fecha límite hasta</label>
                <input type="date" v-model="draftFilters.fechaHasta" />
              </div>

              <div class="field">
                <label>Importe mínimo (€)</label>
                <input type="number" min="0" step="0.01" v-model.number="draftFilters.importeMin" />
              </div>
              <div class="field">
                <label>Importe máximo (€)</label>
                <input type="number" min="0" step="0.01" v-model.number="draftFilters.importeMax" />
              </div>
            </div>

            <div class="checks">
              <label class="chk">
                <input type="checkbox" v-model="draftFilters.soloFavoritas" />
                <span>Solo favoritas</span>
              </label>

              <label class="chk">
                <input type="checkbox" v-model="draftFilters.soloDescartadas" />
                <span>Solo descartadas</span>
              </label>

              <label class="chk">
                <input type="checkbox" v-model="draftFilters.soloVencidas" />
                <span>Solo vencidas</span>
              </label>
            </div>

            <div class="help">
              * Estos filtros se aplican sobre lo que ya devuelve la pestaña actual y el buscador.
            </div>
          </div>

          <div class="modal-foot">
            <button class="btn ghost" type="button" @click="clearFilters">Limpiar</button>
            <button class="btn primary" type="button" @click="applyFilters">Aplicar</button>
          </div>
        </div>
      </div>
      <!-- ✅ /MODAL FILTROS -->
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLicitacionesStore } from '@/stores/licitaciones.store'
import type { Licitacion, Money } from '@/types/licitacion'

const router = useRouter()
const store = useLicitacionesStore()

/**
 * ✅ IMPORTANTE:
 * - Con paginación BACK, la tabla debe usar SOLO la página que devuelve el backend.
 * - Por compatibilidad, esta vista soporta store.rows (recomendado) o store.filtered (fallback).
 */
const backendRows = computed<Licitacion[]>(() => {
  const r = (store as any).rows
  if (Array.isArray(r)) return r as Licitacion[]
  const f = (store as any).filtered
  if (Array.isArray(f)) return f as Licitacion[]
  return []
})

/**
 * ✅ Al cambiar tab o q:
 * - volvemos a page 0
 * - recargamos (store.load ya mandará page/size/q/tab al back)
 */
watch(
  () => [store.tab, store.q],
  () => {
    if (typeof (store as any).setPage === 'function') {
      ;(store as any).setPage(0)
    } else {
      ;(store as any).page = 0
    }
    store.load()
  }
)

onMounted(() => {
  store.load()
})

function onQuery(v: string) {
  // ideal: debounced en el store, pero así funciona
  store.setQuery(v)
}

/* =========================
   ✅ FILTROS MODAL (locales, sobre la página actual)
   ========================= */

type LocalFilters = {
  fechaDesde: string
  fechaHasta: string
  importeMin: number | null
  importeMax: number | null
  soloFavoritas: boolean
  soloDescartadas: boolean
  soloVencidas: boolean
}

const filtersOpen = ref(false)

const appliedFilters = reactive<LocalFilters>({
  fechaDesde: '',
  fechaHasta: '',
  importeMin: null,
  importeMax: null,
  soloFavoritas: false,
  soloDescartadas: false,
  soloVencidas: false
})

const draftFilters = reactive<LocalFilters>({
  fechaDesde: '',
  fechaHasta: '',
  importeMin: null,
  importeMax: null,
  soloFavoritas: false,
  soloDescartadas: false,
  soloVencidas: false
})

function openFilters() {
  draftFilters.fechaDesde = appliedFilters.fechaDesde
  draftFilters.fechaHasta = appliedFilters.fechaHasta
  draftFilters.importeMin = appliedFilters.importeMin
  draftFilters.importeMax = appliedFilters.importeMax
  draftFilters.soloFavoritas = appliedFilters.soloFavoritas
  draftFilters.soloDescartadas = appliedFilters.soloDescartadas
  draftFilters.soloVencidas = appliedFilters.soloVencidas
  filtersOpen.value = true
}
function closeFilters() {
  filtersOpen.value = false
}
function clearFilters() {
  draftFilters.fechaDesde = ''
  draftFilters.fechaHasta = ''
  draftFilters.importeMin = null
  draftFilters.importeMax = null
  draftFilters.soloFavoritas = false
  draftFilters.soloDescartadas = false
  draftFilters.soloVencidas = false
}
function applyFilters() {
  appliedFilters.fechaDesde = draftFilters.fechaDesde
  appliedFilters.fechaHasta = draftFilters.fechaHasta
  appliedFilters.importeMin = draftFilters.importeMin
  appliedFilters.importeMax = draftFilters.importeMax
  appliedFilters.soloFavoritas = draftFilters.soloFavoritas
  appliedFilters.soloDescartadas = draftFilters.soloDescartadas
  appliedFilters.soloVencidas = draftFilters.soloVencidas
  filtersOpen.value = false
}

function toDateOnly(isoOrDate: string): Date | null {
  if (!isoOrDate) return null
  const d = new Date(isoOrDate)
  if (Number.isNaN(d.getTime())) return null
  d.setHours(0, 0, 0, 0)
  return d
}

function getFechaLimiteISO(row: Licitacion): string | null {
  return (
    (row.fechas as any)?.fechaLimite ||
    (row.fechas as any)?.fechaLimitePresentacion ||
    (row.fechas as any)?.fechaFinPresentacionOferta ||
    (row.fechas as any)?.fechaPublicacion ||
    (row as any)?.fechaPublicacion ||
    null
  )
}

function getRowAmount(row: any): number | null {
  const m = row?.presupuestoBase ?? row?.valorEstimado
  const amount = m?.amount
  const n = Number(amount)
  return Number.isFinite(n) ? n : null
}

function isVencida(row: any): boolean {
  if ((row as any).__vencida === true) return true
  const iso = getFechaLimiteISO(row)
  if (!iso) return false
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  d.setHours(0, 0, 0, 0)
  return d.getTime() < today.getTime()
}

function passLocalFilters(row: any): boolean {
  if (appliedFilters.soloFavoritas && !(row as any).__favorita) return false
  if (appliedFilters.soloDescartadas && !(row as any).__descartada) return false
  if (appliedFilters.soloVencidas && !isVencida(row)) return false

  const iso = getFechaLimiteISO(row)
  const rowDate = iso ? toDateOnly(iso) : null

  const desde = toDateOnly(appliedFilters.fechaDesde)
  const hasta = toDateOnly(appliedFilters.fechaHasta)

  if (desde && rowDate && rowDate.getTime() < desde.getTime()) return false
  if (hasta && rowDate && rowDate.getTime() > hasta.getTime()) return false
  if ((desde || hasta) && !rowDate) return false

  const amt = getRowAmount(row)
  if (appliedFilters.importeMin != null) {
    if (amt == null || amt < appliedFilters.importeMin) return false
  }
  if (appliedFilters.importeMax != null) {
    if (amt == null || amt > appliedFilters.importeMax) return false
  }

  return true
}

/**
 * ✅ rows = página del BACK + filtros locales + orden (si quieres conservarlo)
 * Si el back ya ordena, puedes quitar el .sort()
 */
function getFechaOrden(row: Licitacion): Date | null {
  const iso = getFechaLimiteISO(row)
  if (!iso) return null
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? null : d
}

const rows = computed<Licitacion[]>(() => {
  return [...backendRows.value]
    .filter(passLocalFilters)
    .sort((a, b) => {
      const fa = getFechaOrden(a)
      const fb = getFechaOrden(b)
      if (!fa && !fb) return 0
      if (!fa) return 1
      if (!fb) return -1
      return fb.getTime() - fa.getTime()
    })
})

const counts = computed(() => store.counts)

/**
 * ✅ totalAll: del BACK (totalElements). OJO: los filtros locales no cambian totalElements.
 */
const totalAll = computed(() => {
  const te = (store as any).totalElements
  return Number.isFinite(Number(te)) ? Number(te) : backendRows.value.length
})

/**
 * ✅ paginación BACK (store.page 0-based)
 */
const page = computed(() => ((store as any).page ?? 0) + 1)
const totalPages = computed(() => (store as any).totalPages ?? 1)

/**
 * ✅ pageFrom/pageTo en base a page/size del BACK (no a rows.length)
 */
const pageFrom = computed(() => {
  if (totalAll.value === 0) return 0
  const p = Number((store as any).page ?? 0)
  const size = Number((store as any).size ?? 50)
  return p * size + 1
})
const pageTo = computed(() => {
  if (totalAll.value === 0) return 0
  const p = Number((store as any).page ?? 0)
  const size = Number((store as any).size ?? 50)
  return Math.min((p + 1) * size, totalAll.value)
})

function goDetalle(row: Licitacion) {
  router.push(`/licitaciones/${row.id}`)
}

function formatCount(n: any) {
  const v = Number(n ?? 0)
  return v.toLocaleString('es-ES')
}

function displayOrganismo(row: any): string {
  const orgNombre = String(row.organismo_nombre || '').trim()
  if (orgNombre) return orgNombre

  const orgObjNombre = String(row.organismo?.nombre || '').trim()
  if (orgObjNombre) return orgObjNombre

  const org = String(row.organismo || '').trim()
  if (!org || org === '-' || org.toLowerCase() === 'sector público') {
    const organo = String(row.organo || '').trim()
    if (organo) return organo
    const entidad = String(row.entidad || '').trim()
    if (entidad) return entidad
    return '-'
  }
  return org
}

function formatMoney(money: Money | null | undefined, fallbackCurrency: string | null | undefined) {
  if (!money) return '-'
  const amount = (money as any).amount
  const currency = (money as any).currency || fallbackCurrency || 'EUR'
  if (amount === null || amount === undefined) return '-'
  try {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2
    }).format(Number(amount))
  } catch {
    return `${Number(amount).toLocaleString('es-ES')} ${currency}`
  }
}

/* =========================================================
   ✅ CCAA desde código postal (2 primeros dígitos)
   ========================================================= */

const CCAA_BY_PREFIX: Record<string, string> = {
  '04': 'Andalucía','11': 'Andalucía','14': 'Andalucía','18': 'Andalucía',
  '21': 'Andalucía','23': 'Andalucía','29': 'Andalucía','41': 'Andalucía',
  '22': 'Aragón','44': 'Aragón','50': 'Aragón',
  '33': 'Asturias',
  '07': 'Illes Balears',
  '35': 'Canarias','38': 'Canarias',
  '39': 'Cantabria',
  '02': 'Castilla-La Mancha','13': 'Castilla-La Mancha','16': 'Castilla-La Mancha',
  '19': 'Castilla-La Mancha','45': 'Castilla-La Mancha',
  '05': 'Castilla y León','09': 'Castilla y León','24': 'Castilla y León',
  '34': 'Castilla y León','37': 'Castilla y León','40': 'Castilla y León',
  '42': 'Castilla y León','47': 'Castilla y León','49': 'Castilla y León',
  '08': 'Cataluña','17': 'Cataluña','25': 'Cataluña','43': 'Cataluña',
  '03': 'Comunidad Valenciana','12': 'Comunidad Valenciana','46': 'Comunidad Valenciana',
  '06': 'Extremadura','10': 'Extremadura',
  '15': 'Galicia','27': 'Galicia','32': 'Galicia','36': 'Galicia',
  '28': 'Comunidad de Madrid',
  '30': 'Región de Murcia',
  '31': 'Navarra',
  '01': 'País Vasco','20': 'País Vasco','48': 'País Vasco',
  '26': 'La Rioja',
  '51': 'Ceuta',
  '52': 'Melilla'
}

function extractCpPrefix(value: string | null | undefined): string | null {
  if (!value) return null
  const s = String(value).trim()
  const m5 = s.match(/\b(\d{5})\b/)
  const cp5 = m5?.[1]
  if (cp5) return cp5.substring(0, 2)
  const m2 = s.match(/\b(\d{2})\b/)
  const p2 = m2?.[1]
  if (p2) return p2
  const digits = s.replace(/[^\d]/g, '')
  if (digits.length >= 2) return digits.substring(0, 2)
  return null
}

function formatComunidadAutonoma(lugar: string | null | undefined) {
  const prefix = extractCpPrefix(lugar)
  if (!prefix) return '-'
  return CCAA_BY_PREFIX[prefix] ?? '-'
}

function formatFechaLimite(row: Licitacion) {
  const iso = getFechaLimiteISO(row)
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleDateString('es-ES')
}

function pillClass(row: Licitacion) {
  const iso = getFechaLimiteISO(row)
  if (!iso) return 'pill-neutral'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return 'pill-neutral'

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const target = new Date(d)
  target.setHours(0, 0, 0, 0)

  const diffMs = target.getTime() - today.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return 'pill-red'
  if (diffDays <= 1) return 'pill-yellow'
  return 'pill-green'
}

function formatProvincia(lugar: string | null | undefined) {
  return formatComunidadAutonoma(lugar)
}

/* =========================
   ✅ DESCARGAR CSV (página actual)
   ========================= */

function csvEscape(v: any): string {
  const s = String(v ?? '')
  if (/[",\n;]/.test(s)) return `"${s.replace(/"/g, '""')}"`
  return s
}

function downloadCsv() {
  const data = rows.value

  const header = ['id', 'expediente', 'titulo', 'organismo', 'fecha_limite', 'importe', 'provincia']
  const lines = [header.join(';')]

  for (const r of data as any[]) {
    const fecha = formatFechaLimite(r)
    const importe = formatMoney(r.presupuestoBase ?? r.valorEstimado, r.moneda)
    const provincia = formatProvincia(r.lugarEjecucion)

    lines.push(
      [
        csvEscape(r.id),
        csvEscape(r.expediente),
        csvEscape(r.titulo),
        csvEscape(displayOrganismo(r)),
        csvEscape(fecha),
        csvEscape(importe),
        csvEscape(provincia)
      ].join(';')
    )
  }

  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `licitaciones_${store.tab}_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
/* (Tu CSS tal cual; lo dejo idéntico para que no cambie nada visual) */
.page {
  padding: 22px;
  background: #f5f7fb;
  min-height: calc(100vh - 56px);
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif;

}

.header h1 {
  margin: 0 0 14px;
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.3px;
  color: #1f2a44;
}

/* ✅ SIN borde azul */
.card {
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e7edf6; /* sutil */
  box-shadow: 0 18px 50px rgba(16, 24, 40, 0.1);
  padding: 14px 16px 12px;

  /* ✅ para que el área tabla sea scroll si se pasa de alto */
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 56px - 22px - 22px - 54px); /* ajuste conservador */
}

/* TABS */
.tabs {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 4px 6px 0;
  border-bottom: 1px solid #e7edf6;
  flex-wrap: wrap;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  border: 0;
  padding: 12px 2px 12px;
  cursor: pointer;
  color: #6b7280;
  font-weight: 900;
  letter-spacing: 0.2px;
  position: relative;
}

.tab-ico {
  width: 18px;
  height: 18px;
  display: inline-grid;
  place-items: center;
  color: currentColor;
}
.tab-ico svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
  opacity: 0.9;
}

.tab.active {
  color: #0b5ed7;
}
.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  background: #0b5ed7;
  border-radius: 6px 6px 0 0;
}

/* TOOLBAR */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 6px 10px;
  flex-wrap: wrap;
}

.search {
  flex: 1;
  min-width: 280px;
  max-width: 720px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f3f6fb;
  border-radius: 10px;
  padding: 10px 12px;
}

.search-ico {
  width: 18px;
  height: 18px;
  display: inline-grid;
  place-items: center;
  color: #9aa4b2;
}
.search-ico svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

.search input {
  width: 100%;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #1f2a44;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  font-weight: 900;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn-ico {
  width: 16px;
  height: 16px;
  display: inline-grid;
  place-items: center;
}
.btn-ico svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.btn.ghost {
  background: #fff;
  color: #0b5ed7;
  border-color: #0b5ed7;
}
.btn.primary {
  background: #0b5ed7;
  color: #fff;
  border-color: #0b5ed7;
}

.state {
  padding: 12px 6px;
  color: #6b7280;
}
.state.error {
  color: #b91c1c;
  font-weight: 900;
}

/* ✅ Área tabla: ocupa el resto y hace scroll vertical */
.table-area {
  display: flex;
  flex-direction: column;
  min-height: 0; /* clave para scroll dentro de flex */
  flex: 1;
}

.table-wrap {
  margin-top: 6px;
  border-top: 1px solid #e7edf6;
  padding-top: 6px;

  /* ✅ SOLO scroll vertical; sin horizontal */
  overflow-y: auto;
  overflow-x: hidden;

  min-height: 0;
  flex: 1;

  width: 100%;
}

/* ✅ Tabla: sin min-width fijo para evitar scroll horizontal */
.table {
  width: 100%;
  border-collapse: collapse;

  min-width: 0; /* antes 920px -> provocaba scroll */
  table-layout: fixed;
}

thead th {
  text-align: left;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.9px;
  color: #9aa4b2;
  padding: 14px 10px 10px;
  border-bottom: 2px solid #e7edf6;

  white-space: nowrap;

  position: sticky;
  top: 0;
  background: #fff;
  z-index: 2;
}

.sort {
  margin-left: 6px;
  color: #b7c0cf;
  font-weight: 900;
}

tbody td {
  padding: 16px 10px;
  border-bottom: 2px solid #eef2f7;
  vertical-align: top;
  color: #2b3550;
  font-size: 13px;

  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ✅ Anchos “fluid” con clamp (igual que tenías) */
.col-interes {
  width: clamp(92px, 10vw, 120px);
}
.col-exp {
  width: auto;
}
.col-org {
  width: clamp(150px, 22vw, 340px);
}
.col-fecha {
  width: clamp(110px, 12vw, 160px);
}
.col-importe {
  width: clamp(130px, 12vw, 180px);
}
.col-prov {
  width: clamp(120px, 12vw, 160px);
}

/* ICONOS circulares */
.icon-circle {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 2px solid #cdd6e4;
  background: #fff;
  display: inline-grid;
  place-items: center;
  margin-right: 10px;
  cursor: pointer;
  color: #6b7280;
}
.icon-circle svg {
  width: 16px;
  height: 16px;
  fill: currentColor;
  opacity: 0.9;
}
.icon-circle:hover {
  border-color: #0b5ed7;
  color: #0b5ed7;
}

/* Badge expediente */
.exp-badge {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef3fb;
  color: #2b3550;
  font-weight: 900;
  font-size: 12px;
  margin-bottom: 8px;
  max-width: 100%;
}

/* Link título (ya clampa 2 líneas) */
.link {
  display: block;
  color: #1b62ff;
  text-decoration: underline;
  font-weight: 800;
  line-height: 1.25;

  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;

  overflow-wrap: anywhere;
  word-break: break-word;
}

.org {
  color: #2b3550;
  line-height: 1.25;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;

  overflow-wrap: anywhere;
  word-break: break-word;
}

/* FECHA pill */
.date-pill {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 10px;
  font-weight: 900;
  font-size: 12px;
}
.pill-red {
  background: rgba(239, 68, 68, 0.18);
  color: #b42318;
}
.pill-yellow {
  background: rgba(245, 158, 11, 0.18);
  color: #b45309;
}
.pill-green {
  background: rgba(34, 197, 94, 0.18);
  color: #15803d;
}
.pill-neutral {
  background: rgba(148, 163, 184, 0.2);
  color: #475569;
}

.col-importe {
  font-weight: 900;
}

.empty {
  padding: 18px 10px;
  color: #6b7280;
}

/* ✅ Paginación */
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 6px 0;
  border-top: 1px solid #e7edf6;
  margin-top: 6px;
  flex-wrap: wrap;
}

.pager-info {
  color: #6b7280;
  font-size: 13px;
}

.pager-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pbtn {
  height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #d8e1ef;
  background: #fff;
  color: #1f2a44;
  font-weight: 900;
  cursor: pointer;
}
.pbtn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-pill {
  height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  background: #f3f6fb;
  color: #1f2a44;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
}

/* =========================
   ✅ MODAL FILTROS
   ========================= */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.55);
  display: grid;
  place-items: center;
  padding: 18px;
  z-index: 9999;
}

.modal {
  width: min(760px, 96vw);
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e7edf6;
  box-shadow: 0 18px 60px rgba(16, 24, 40, 0.25);
  overflow: hidden;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #e7edf6;
}

.modal-title {
  font-weight: 900;
  color: #1f2a44;
}

.icon-x {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid #d8e1ef;
  background: #fff;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  color: #6b7280;
}
.icon-x svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

.modal-body {
  padding: 14px 16px 10px;
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 14px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 800;
  color: #6b7280;
  margin-bottom: 6px;
}

.field input {
  width: 100%;
  height: 38px;
  border-radius: 10px;
  border: 1px solid #d8e1ef;
  background: #fff;
  padding: 0 12px;
  font-size: 13px;
  color: #1f2a44;
  outline: none;
}
.field input:focus {
  border-color: #0b5ed7;
  box-shadow: 0 0 0 4px rgba(11, 94, 215, 0.12);
}

.checks {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.chk {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2b3550;
  font-weight: 800;
}
.chk input {
  width: 18px;
  height: 18px;
}

.help {
  margin-top: 10px;
  font-size: 12px;
  color: #9aa4b2;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #e7edf6;
  background: #fff;
}

@media (max-width: 720px) {
  .grid2 {
    grid-template-columns: 1fr;
  }
}

/* =========================
   RESPONSIVE SIN SCROLL HORIZONTAL
   ========================= */

@media (max-width: 1200px) {
  .col-prov {
    display: none;
  }
}

@media (max-width: 1024px) {
  .col-importe {
    display: none;
  }
}

@media (max-width: 860px) {
  .col-org {
    display: none;
  }

  .col-exp {
    width: auto;
  }
}

@media (max-width: 980px) {
  .tabs {
    gap: 16px;
    padding-bottom: 6px;
  }
  .search {
    min-width: 240px;
    max-width: 100%;
  }
  .actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 600px) {
  .page {
    padding: 14px;
  }
  .header h1 {
    font-size: 22px;
  }
  .card {
    padding: 12px;
  }

  thead th {
    padding: 12px 8px 10px;
  }
  tbody td {
    padding: 14px 8px;
  }
}
</style>
