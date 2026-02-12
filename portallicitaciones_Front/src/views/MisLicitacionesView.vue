<!-- src/views/MisLicitacionesView.vue -->
<template>
  <div class="page">
    <header class="header">
      <h1>MIS LICITACIONES</h1>
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


          <button class="btn primary" type="button" @click="descargar">
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
              <tr
                v-for="row in pagedRows"
                :key="row.uuid ?? row.id"
                class="row-click"
                role="button"
                tabindex="0"
                @click="goDetalle(row)"
                @keydown.enter.prevent="goDetalle(row)"
                @keydown.space.prevent="goDetalle(row)"
              >
                <td class="col-interes">
                  <!-- DESCARTAR -->
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

                  <!-- FAVORITA -->
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
                  <div class="exp-badge">{{ displayExpediente(row) }}</div>

                  <!-- ✅ Título: navega al detalle -->
                  <a class="link" href="#" @click.prevent.stop="goDetalle(row)">
                    {{ row.titulo || '—' }}
                  </a>
                </td>

                <td class="col-org">
                  <!-- ✅ AHORA sale del join por organismo_id -->
                  <div class="org" :title="organismoTitle(row)">
                    {{ displayOrganismo(row) }}
                  </div>
                  <div v-if="displayOrganismoNif(row)" class="org-sub">
                    NIF: {{ displayOrganismoNif(row) }}
                  </div>
                </td>

                <td class="col-fecha">
                  <span class="date-pill" :class="pillClass(row)">{{ formatFechaLimite(row) }}</span>
                </td>

                <td class="col-importe">
                  {{ formatMoney(row.importe) }}
                </td>

                <td class="col-prov">
                  {{ formatProvincia() }}
                </td>
              </tr>

              <tr v-if="pagedRows.length === 0">
                <td colspan="6" class="empty">No hay resultados.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination footer -->
        <div class="pager">
          <div class="pager-info">
            Mostrando
            <strong>{{ pageFrom }}</strong>–<strong>{{ pageTo }}</strong>
            de <strong>{{ totalRows }}</strong>
          </div>

          <div class="pager-actions">
            <button class="pbtn" type="button" :disabled="page === 1" @click="prevPage">
              Anterior
            </button>

            <div class="page-pill">
              Página <strong>{{ page }}</strong> / <strong>{{ totalPages }}</strong>
            </div>

            <button class="pbtn" type="button" :disabled="page === totalPages" @click="nextPage">
              Siguiente
            </button>
          </div>
        </div>
      </div>

      <!-- MODAL FILTROS -->
<div v-if="filtersOpen" class="modal-backdrop" @click.self="closeFilters">
  <div class="modal" role="dialog" aria-modal="true" aria-label="Filtros">
    <div class="modal-head">
      <div class="modal-title">Filtros</div>
      <button class="xbtn" type="button" @click="closeFilters" aria-label="Cerrar">✕</button>
    </div>

    <div class="modal-body">
      <div class="fgrid">
        <!-- Fecha límite -->
        <div class="frow">
          <label>Fecha límite desde</label>
          <input type="date" v-model="uiFilters.fechaDesde" />
        </div>

        <div class="frow">
          <label>Fecha límite hasta</label>
          <input type="date" v-model="uiFilters.fechaHasta" />
        </div>

        <!-- Importe -->
        <div class="frow">
          <label>Importe mínimo (€)</label>
          <input type="number" inputmode="decimal" v-model="uiFilters.importeMin" placeholder="0" />
        </div>

        <div class="frow">
          <label>Importe máximo (€)</label>
          <input type="number" inputmode="decimal" v-model="uiFilters.importeMax" placeholder="0" />
        </div>

        <!-- Flags -->
        <div class="frow checkrow">
          <label class="check">
            <input type="checkbox" v-model="uiFilters.soloFavoritas" />
            <span>Solo favoritas</span>
          </label>
        </div>

        <div class="frow checkrow">
          <label class="check">
            <input type="checkbox" v-model="uiFilters.soloDescartadas" />
            <span>Solo descartadas</span>
          </label>
        </div>

        <div class="frow checkrow">
          <label class="check">
            <input type="checkbox" v-model="uiFilters.soloVencidas" />
            <span>Solo vencidas</span>
          </label>
        </div>

        <div class="hint">
          * Estos filtros se aplican sobre lo que ya devuelve la pestaña actual y el buscador.
        </div>
      </div>
    </div>

    <div class="modal-foot">
      <button class="btn2 ghost" type="button" @click="limpiarFiltros">Limpiar</button>
      <button class="btn2" type="button" @click="aplicarFiltros">Aplicar</button>
    </div>
  </div>
</div>




    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useMisLicitacionesStore, type DecoratedRow } from "@/stores/misLicitaciones.store";

const router = useRouter();
const store = useMisLicitacionesStore();

/** ✅ paginación (UI) */
const PAGE_SIZE = 25;
const page = ref(1);

/** ✅ reset de página cuando cambia el filtro/tab/búsqueda */
watch(
  () => [store.tab, store.q, store.filtered.length],
  () => {
    page.value = 1;
  }
);

onMounted(() => {
  store.load();
});

function csvEscape(v: any) {
  const s = String(v ?? "");
  // comillas dobles duplicadas
  const safe = s.replace(/"/g, '""');
  return `"${safe}"`;
}

function toIsoDateOrDash(iso: any) {
  const s = String(iso ?? "").trim();
  if (!s) return "-";
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return s;
  // YYYY-MM-DD
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

const filtersOpen = ref(false);

const uiFilters = ref({
  fechaDesde: "",     // YYYY-MM-DD
  fechaHasta: "",     // YYYY-MM-DD
  importeMin: "",     // string num
  importeMax: "",     // string num
  soloFavoritas: false,
  soloDescartadas: false,
  soloVencidas: false,
});

const appliedFilters = ref({ ...uiFilters.value });

function openFilters() {
  // al abrir, clonamos los aplicados para que puedas cancelar sin tocar lo aplicado
  uiFilters.value = { ...appliedFilters.value };
  filtersOpen.value = true;
}

function closeFilters() {
  filtersOpen.value = false;
}

function aplicarFiltros() {
  appliedFilters.value = { ...uiFilters.value };
  filtersOpen.value = false;
  page.value = 1;
}

function limpiarFiltros() {
  uiFilters.value = {
    fechaDesde: "",
    fechaHasta: "",
    importeMin: "",
    importeMax: "",
    soloFavoritas: false,
    soloDescartadas: false,
    soloVencidas: false,
  };
  appliedFilters.value = { ...uiFilters.value };
  page.value = 1;
}

function parseDateYMD(ymd: string): Date | null {
  if (!ymd) return null;
  const d = new Date(ymd + "T00:00:00");
  return Number.isNaN(d.getTime()) ? null : d;
}

function getFechaLimiteDate(row: any): Date | null {
  const iso = getFechaLimiteISO(row);
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

function numOrNull(v: any): number | null {
  const s = String(v ?? "").trim();
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}



function downloadTextFile(filename: string, content: string, mime = "text/csv;charset=utf-8;") {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();

  URL.revokeObjectURL(url);
}

/**
 * ✅ Descarga dinámica:
 * - Usa EXACTAMENTE lo que se muestra (store.tab + store.q + filtros => rows)
 * - Por defecto descarga TODOS los resultados filtrados (no solo la página).
 *   Si quieres solo la página visible: usa pagedRows.value
 */
function descargar() {
  const data = rows.value; // <- TODOS los resultados filtrados y buscados
  // const data = pagedRows.value; // <- SOLO la página actual (si lo prefieres)

  const header = [
    "TAB",
    "EXPEDIENTE",
    "TITULO",
    "ORGANISMO",
    "NIF_ORGANISMO",
    "FECHA_LIMITE",
    "IMPORTE",
    "PROVINCIA",
    "FAVORITA",
    "DESCARTADA",
    "VENCIDA",
    "UUID",
  ].join(";");

  const lines = data.map((row: any) => {
    const tab = store.tab;

    const expediente = displayExpediente(row);
    const titulo = row.titulo ?? "";
    const organismo = displayOrganismo(row);
    const nif = displayOrganismoNif(row) ?? "";
    const fechaLimite = toIsoDateOrDash(getFechaLimiteISO(row));
    const importe = row.importe ?? "";
    const provincia = formatProvincia();

    const favorita = (row as any).__favorita ? "SI" : "NO";
    const descartada = (row as any).__descartada ? "SI" : "NO";
    const vencida = (row as any).__vencida ? "SI" : "NO";

    const uuid = String(row.uuid ?? (row as any).id ?? "").trim();

    return [
      csvEscape(tab),
      csvEscape(expediente),
      csvEscape(titulo),
      csvEscape(organismo),
      csvEscape(nif),
      csvEscape(fechaLimite),
      csvEscape(importe),
      csvEscape(provincia),
      csvEscape(favorita),
      csvEscape(descartada),
      csvEscape(vencida),
      csvEscape(uuid),
    ].join(";");
  });

  const today = new Date();
  const y = today.getFullYear();
  const m = String(today.getMonth() + 1).padStart(2, "0");
  const d = String(today.getDate()).padStart(2, "0");

  const q = (store.q ?? "").trim();
  const qTag = q ? `_q-${q.replace(/[^a-z0-9]+/gi, "_").slice(0, 30)}` : "";

  const filename = `mis_licitaciones_${store.tab}_${y}-${m}-${d}${qTag}.csv`;

  // BOM para Excel ES
  const csv = "\uFEFF" + [header, ...lines].join("\n");
  downloadTextFile(filename, csv);
}


function onQuery(v: string) {
  store.setQuery(v);
}

const rows = computed(() => {
  const f = appliedFilters.value;

  // base: lo que ya viene filtrado por el store (tab + q + lo que sea)
  let out = [...store.filtered];

  // flags
  if (f.soloFavoritas) out = out.filter((r: any) => !!r.__favorita);
  if (f.soloDescartadas) out = out.filter((r: any) => !!r.__descartada);
  if (f.soloVencidas) out = out.filter((r: any) => !!r.__vencida);

  // fecha límite rango
  const dFrom = parseDateYMD(f.fechaDesde);
  const dTo = parseDateYMD(f.fechaHasta);
  if (dFrom) out = out.filter((r: any) => {
    const dl = getFechaLimiteDate(r);
    return dl ? dl.getTime() >= dFrom.getTime() : false;
  });
  if (dTo) out = out.filter((r: any) => {
    const dl = getFechaLimiteDate(r);
    return dl ? dl.getTime() <= dTo.getTime() : false;
  });

  // importe rango
  const min = numOrNull(f.importeMin);
  const max = numOrNull(f.importeMax);
  if (min != null) out = out.filter((r: any) => Number(r.importe ?? 0) >= min);
  if (max != null) out = out.filter((r: any) => Number(r.importe ?? 0) <= max);

  // sort (tu lógica)
  out.sort((a, b) => {
    const fa = getFechaOrden(a);
    const fb = getFechaOrden(b);

    if (!fa && !fb) return 0;
    if (!fa) return 1;
    if (!fb) return -1;

    return fb.getTime() - fa.getTime();
  });

  return out;
});


const counts = computed(() => store.counts);

const totalRows = computed(() => rows.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(totalRows.value / PAGE_SIZE)));

const pagedRows = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE;
  return rows.value.slice(start, start + PAGE_SIZE);
});

const pageFrom = computed(() => {
  if (totalRows.value === 0) return 0;
  return (page.value - 1) * PAGE_SIZE + 1;
});

const pageTo = computed(() => {
  return Math.min(page.value * PAGE_SIZE, totalRows.value);
});

function prevPage() {
  page.value = Math.max(1, page.value - 1);
}

function nextPage() {
  page.value = Math.min(totalPages.value, page.value + 1);
}

/** ✅ Ir al detalle interno por UUID */
function goDetalle(row: DecoratedRow) {
  const id = String((row.uuid ?? (row as any).id) ?? "").trim();
  if (!id) return;
  router.push(`/licitaciones/${id}`);
}

function formatCount(n: any) {
  const v = Number(n ?? 0);
  return v.toLocaleString("es-ES");
}

function getFechaOrden(row: any): Date | null {
  const iso = row.fecha_creacion || row.fecha_limite || null;
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

function displayExpediente(row: any): string {
  const v = String(row.id_externo || "").trim();
  return v || "—";
}

/**
 * ✅ ORGANISMO desde FK organismo_id:
 * - si el back devuelve join: row.organismo = { id, nombre, nif, codigo... }
 * - si no, intentamos compatibilidad: row.organismoNombre / row.organismo_nombre / row.organismo (string)
 */
function displayOrganismo(row: any): string {
  // 1) caso ideal: row.organismo objeto
  const o = row?.organismo;
  if (o && typeof o === "object") {
    const nombre = String(o.nombre ?? "").trim();
    if (nombre) return nombre;
    const codigo = String(o.codigo ?? "").trim();
    if (codigo) return codigo;
  }

  // 2) caso: back manda nombre suelto (muy típico)
  const nombreAlt = String(
    row?.organismoNombre ??
    row?.organismo_nombre ??
    row?.organismo_name ??
    row?.organismoNombreFk ??
    ""
  ).trim();
  if (nombreAlt) return nombreAlt;

  // 3) caso: tu tabla usuario_licitaciones ya trae "organismo" como texto
  const orgStr = String(row?.organismo ?? "").trim();
  if (orgStr) return orgStr;

  // 4) caso: SOLO viene organismo_id → no se puede resolver sin back o caché
  const id = row?.organismo_id ?? row?.organismoId ?? null;
  if (id != null) return `Organismo #${id}`;

  return "-";
}

function displayOrganismoNif(row: any): string | null {
  const o = row?.organismo;
  if (o && typeof o === "object") {
    const nif = String(o.nif ?? "").trim();
    return nif || null;
  }
  const nifAlt = String(row?.organismoNif ?? row?.organismo_nif ?? "").trim();
  return nifAlt || null;
}


function organismoTitle(row: any): string {
  const nombre = displayOrganismo(row);
  const nif = displayOrganismoNif(row);
  return nif ? `${nombre} · NIF: ${nif}` : nombre;
}

function formatMoney(importe: number | null | undefined) {
  if (importe === null || importe === undefined) return "-";
  try {
    return new Intl.NumberFormat("es-ES", {
      style: "currency",
      currency: "EUR",
      maximumFractionDigits: 2,
    }).format(Number(importe));
  } catch {
    return `${Number(importe).toLocaleString("es-ES")} €`;
  }
}

function formatProvincia() {
  return "-";
}

function getFechaLimiteISO(row: any): string | null {
  return row.fecha_limite || null;
}

function formatFechaLimite(row: any) {
  const iso = getFechaLimiteISO(row);
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);
  return d.toLocaleDateString("es-ES");
}

function pillClass(row: any) {
  const iso = getFechaLimiteISO(row);
  if (!iso) return "pill-neutral";

  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "pill-neutral";

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const target = new Date(d);
  target.setHours(0, 0, 0, 0);

  const diffMs = target.getTime() - today.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays < 0) return "pill-red";
  if (diffDays <= 1) return "pill-yellow";
  return "pill-green";
}
</script>

<style scoped>
.page {
  padding: 22px;
  background: #f5f7fb;
  min-height: calc(100vh - 56px);
  font-family: Inter, system-ui, Arial, sans-serif;
}

.header h1 {
  margin: 0 0 14px;
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.3px;
  color: #1f2a44;
}

.card {
  background: #fff;
  border-radius: 18px;
  border: 1px solid #e7edf6;
  box-shadow: 0 18px 50px rgba(16, 24, 40, 0.1);
  padding: 14px 16px 12px;

  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 56px - 22px - 22px - 54px);
}

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
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  background: #0b5ed7;
  border-radius: 6px 6px 0 0;
}

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

.table-area {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.table-wrap {
  margin-top: 6px;
  border-top: 1px solid #e7edf6;
  padding-top: 6px;

  overflow-y: auto;
  overflow-x: hidden;

  min-height: 0;
  flex: 1;

  width: 100%;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 0;
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


/* =========================
   MODAL FILTROS (look & feel Ordenatec)
   ========================= */

/* overlay */
.fmodal-overlay{
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 18px;
}

/* caja */
.fmodal{
  width: min(860px, 100%);
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e7edf6;
  box-shadow: 0 24px 70px rgba(16,24,40,.18);
  overflow: hidden;
  font-family: Inter, system-ui, Arial, sans-serif;
}

/* header */
.fmodal-head{
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.fmodal-title{
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: .2px;
  color: #1f2a44;
}

/* close button */
.fmodal-close{
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid #d8e1ef;
  background: #fff;
  color: #1f2a44;
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  font-weight: 900;
}
.fmodal-close:hover{
  border-color: #0b5ed7;
  color: #0b5ed7;
  box-shadow: 0 10px 22px rgba(11,94,215,.10);
}

/* body */
.fmodal-body{
  padding: 16px;
}

/* grid 2 columnas */
.fmodal-grid{
  display: grid;
  grid-template-columns: repeat(2, minmax(0,1fr));
  gap: 14px 16px;
  align-items: start;
}

/* campos */
.fmodal-field label{
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #6b7280;
  margin-bottom: 8px;
}

/* inputs */
.fmodal-input{
  width: 100%;
  height: 42px;
  border: 1px solid #d8e1ef;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 13px;
  background: #fff;
  color: #111827;
  box-sizing: border-box;
}

.fmodal-input:focus{
  outline: none;
  border-color: #0b5ed7;
  box-shadow: 0 0 0 3px rgba(11,94,215,.12);
}

/* input con icono (si lo usas) */
.fmodal-inputwrap{
  position: relative;
}
.fmodal-inputwrap .fmodal-input{
  padding-right: 38px;
}
.fmodal-ico{
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: #9aa4b2;
  pointer-events: none;
}
.fmodal-ico svg{
  width: 18px;
  height: 18px;
  fill: currentColor;
}

/* separador suave */
.fmodal-sep{
  grid-column: 1 / -1;
  border-top: 1px solid #eef2f7;
  margin: 6px 0 2px;
}

/* switches / checks alineados a tu estilo */
.fmodal-toggles{
  grid-column: 1 / -1;
  display: grid;
  gap: 12px;
  margin-top: 6px;
}

/* fila toggle */
.fmodal-toggle{
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fbfdff;
}

.fmodal-toggle .txt{
  font-size: 13px;
  font-weight: 900;
  color: #2b3550;
}

/* switch azul (reusado del estilo que ya tenías) */
.fmodal-switch{
  position: relative;
  width: 46px;
  height: 26px;
  display: inline-block;
  flex: 0 0 auto;
}
.fmodal-switch input{ display:none; }

.fmodal-slider{
  position:absolute;
  inset:0;
  border-radius:999px;
  background: rgba(37, 99, 235, 0.20);
  border: 1px solid rgba(37, 99, 235, 0.30);
  transition:.2s;
}
.fmodal-slider:before{
  content:"";
  position:absolute;
  width:20px;
  height:20px;
  left:3px;
  top:2px;
  border-radius:50%;
  background:#fff;
  box-shadow:0 2px 8px rgba(0,0,0,.18);
  transition:.2s;
}
.fmodal-switch input:checked + .fmodal-slider{
  background:#0b5ed7;
  border-color:#0b5ed7;
}
.fmodal-switch input:checked + .fmodal-slider:before{
  transform: translateX(20px);
}

/* hint */
.fmodal-hint{
  margin-top: 10px;
  font-size: 12px;
  color: #8a93a3;
  font-weight: 700;
}

/* footer */
.fmodal-foot{
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

/* botones tipo tu toolbar */
.fmodal-btn{
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  font-weight: 900;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid transparent;
}

.fmodal-btn.ghost{
  background: #fff;
  color: #0b5ed7;
  border-color: #0b5ed7;
}
.fmodal-btn.ghost:hover{
  box-shadow: 0 10px 22px rgba(11,94,215,.12);
}

.fmodal-btn.primary{
  background: #0b5ed7;
  color: #fff;
  border-color: #0b5ed7;
}
.fmodal-btn.primary:hover{
  box-shadow: 0 10px 22px rgba(11,94,215,.18);
}

/* responsive */
@media (max-width: 760px){
  .fmodal-grid{
    grid-template-columns: 1fr;
  }
}

/* ===== MODAL FILTROS ===== */
.modal-backdrop{
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  z-index: 2000;
  padding: 16px;
}

.modal{
  width: min(720px, 100%);
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e7edf6;
  box-shadow: 0 22px 60px rgba(16, 24, 40, 0.22);
  overflow: hidden;
}

.modal-head{
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.modal-title{
  font-weight: 1000;
  color: #1f2a44;
  letter-spacing: .2px;
}

.xbtn{
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid #d8e1ef;
  background: #fff;
  cursor: pointer;
  font-weight: 900;
}

.modal-body{
  padding: 14px 16px 6px;
}

.fgrid{
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.frow label{
  display: block;
  font-size: 12px;
  color: #6b7280;
  font-weight: 800;
  margin-bottom: 8px;
}

.frow input{
  width: 100%;
  height: 40px;
  border: 1px solid #d8e1ef;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 13px;
}

.checkrow{
  grid-column: span 2;
}

.check{
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  color: #1f2a44;
}

.hint{
  grid-column: span 2;
  font-size: 12px;
  color: #7b869a;
  padding-top: 4px;
}

.modal-foot{
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #eef2f7;
}

.btn2{
  height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #0b5ed7;
  background: #0b5ed7;
  color: #fff;
  font-weight: 1000;
  cursor: pointer;
}

.btn2.ghost{
  background: #fff;
  color: #0b5ed7;
}

@media (max-width: 640px){
  .fgrid{ grid-template-columns: 1fr; }
  .checkrow{ grid-column: auto; }
  .hint{ grid-column: auto; }
}



.row-click {
  cursor: pointer;
}
.row-click:hover {
  background: rgba(11, 94, 215, 0.04);
}

.col-interes {
  width: clamp(92px, 10vw, 120px);
}
.col-exp {
  width: clamp(360px, 38vw, 560px);
}
.col-org {
  width: clamp(220px, 22vw, 340px);
}
.col-fecha {
  width: clamp(120px, 12vw, 160px);
}
.col-importe {
  width: clamp(130px, 12vw, 180px);
}
.col-prov {
  width: clamp(120px, 12vw, 160px);
}

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





/* ✅ NIF debajo */
.org-sub {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 900;
  color: rgba(43, 53, 80, 0.65);
}

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
