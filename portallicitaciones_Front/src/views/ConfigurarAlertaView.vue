<template>
  <div class="page">
    <div class="kicker">CONFIGURAR ALERTAS (1)</div>

    <div class="content">
      <h1 class="title">CONFIGURAR ALERTA</h1>

      <div class="panel">
        <div class="tabs">
          <button class="tab" :class="{ active: tab === 'params' }" type="button" @click="tab = 'params'">
            <span class="ico">⚙</span>
            PARÁMETROS
          </button>

          <button class="tab" :class="{ active: tab === 'envio' }" type="button" @click="tab = 'envio'">
            <span class="ico">✈</span>
            OPCIONES DE ENVÍO
          </button>
        </div>

        <!-- Error -->
        <div v-if="errorMsg" class="error-box">
          {{ errorMsg }}
        </div>

        <!-- PARÁMETROS -->
        <div v-if="tab === 'params'" class="form">
          <div class="grid">
            <div class="col">
              <div class="field">
                <label>Palabras clave</label>
                <input v-model.trim="form.palabrasClave" type="text" />
              </div>

              <div class="field">
                <label>Descripción de la actividad</label>
                <input v-model.trim="form.descripcionActividad" type="text" />
              </div>

              <div class="field">
                <label>Tipos de contratos</label>
                <input v-model.trim="form.tiposContratos" type="text" />
              </div>

              <div class="field toggle-row">
                <div class="toggle-title">Contratos menores</div>
                <label class="switch">
                  <input type="checkbox" v-model="form.contratosMenores" />
                  <span class="slider"></span>
                </label>
              </div>

              <button class="btn-save" :disabled="saving" type="button" @click="guardar">
                {{ saving ? "Guardando..." : "Guardar cambios" }}
              </button>
            </div>

            <div class="col">
              <div class="field">
                <label>Lugares</label>
                <input v-model.trim="form.lugares" type="text" />
              </div>

              <div class="field">
                <label>Órganos de contratación</label>
                <input v-model.trim="form.organosContratacion" type="text" />
              </div>

              <div class="row2">
                <div class="field">
                  <label>Presupuesto mínimo</label>
                  <input v-model.trim="form.presupuestoMin" type="text" />
                </div>

                <div class="field">
                  <label>Presupuesto máximo</label>
                  <input v-model.trim="form.presupuestoMax" type="text" />
                </div>
              </div>

              <div class="field">
                <label>Sector de actividad CPV</label>
                <input v-model.trim="form.sectorCpv" type="text" />
              </div>
            </div>
          </div>
        </div>

        <!-- OPCIONES DE ENVÍO -->
        <div v-else class="form envio">
          <div class="envio-wrap">
            <div class="field">
              <label>Nombre de la alerta</label>
              <input v-model.trim="envio.nombreAlerta" type="text" />
            </div>

            <div class="row3">
              <div class="field">
                <label>Frecuencia de envío</label>
                <select v-model="envio.frecuencia">
                  <option value="DIARIAMENTE">Diariamente</option>
                  <option value="SEMANALMENTE">Semanalmente</option>
                  <option value="INMEDIATO">Inmediato</option>
                </select>
              </div>

              <div class="field">
                <label>Hora</label>
                <select v-model="envio.hora">
                  <option v-for="h in horas" :key="h" :value="h">{{ h }}</option>
                </select>
              </div>

              <div class="field">
                <label>Día de la semana</label>
                <select v-model="envio.diaSemana" :disabled="envio.frecuencia !== 'SEMANALMENTE'">
                  <option value="Lunes">Lunes</option>
                  <option value="Martes">Martes</option>
                  <option value="Miércoles">Miércoles</option>
                  <option value="Jueves">Jueves</option>
                  <option value="Viernes">Viernes</option>
                  <option value="Sábado">Sábado</option>
                  <option value="Domingo">Domingo</option>
                </select>
              </div>
            </div>

            <div class="toggle-line">
              <div class="toggle-text">Recibir correos cuando alguna licitación tenga cambios</div>

              <label class="switch-blue">
                <input type="checkbox" v-model="envio.notificarCambios" />
                <span class="slider-blue"></span>
              </label>
            </div>

            <div class="emails">
              <label class="emails-label">Emails que reciben alertas</label>

              <input v-model.trim="envio.email1" type="text" placeholder="Email 1" />
              <input v-model.trim="envio.email2" type="text" placeholder="Email 2" />
              <input v-model.trim="envio.email3" type="text" placeholder="Email 3" />
            </div>

            <button class="btn-save" :disabled="saving" type="button" @click="guardar">
              {{ saving ? "Guardando..." : "Guardar cambios" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { alertasApi, type AlertaCreateRequest, type TipoAlerta } from "@/services/alertasApi";

const route = useRoute();
const router = useRouter();

const tab = ref<"params" | "envio">("params");

// id si existe
const id = route.params.id ? Number(route.params.id) : null;
const isNew = id == null;

const saving = ref(false);
const errorMsg = ref("");

const form = reactive({
  palabrasClave: "",
  descripcionActividad: "",
  tiposContratos: "",
  contratosMenores: false,
  lugares: "España",
  organosContratacion: "Todos",
  presupuestoMin: "-",
  presupuestoMax: "-",
  sectorCpv: "", // CPVs
});

const envio = reactive({
  nombreAlerta: "",
  frecuencia: "DIARIAMENTE",
  hora: "10:00",
  diaSemana: "Lunes",
  notificarCambios: true,
  email1: "",
  email2: "",
  email3: "",
});

const horas = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0") + ":00");

function trim(v: any) {
  return String(v ?? "").trim();
}

function parseNumberOrNull(v: string) {
  const s = trim(v);
  if (!s || s === "-") return null;

  // "1.234,56" -> "1234.56"
  const cleaned = s.replace(/[€\s]/g, "").replace(/\./g, "").replace(",", ".");
  const n = Number(cleaned);
  return Number.isFinite(n) ? n : null;
}

function isValidEmail(e: string) {
  const v = trim(e);
  if (!v) return true; // vacío permitido
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

function buildPayload(): AlertaCreateRequest {
  const nombre = trim(envio.nombreAlerta);

  const cpvs = trim(form.sectorCpv);

  // Mantén descripcion como "resumen" para la tabla/compatibilidad
  // (la descripción real va en descripcionActividad)
  const descripcion = trim(form.descripcionActividad) || trim(form.palabrasClave) || nombre;

  const tipo: TipoAlerta = "SERVICIOS";

  return {
    // ✅ NUEVO
    nombre,

    // base
    cpvs,
    descripcion,
    tipo,
    importeMin: parseNumberOrNull(form.presupuestoMin),
    importeMax: parseNumberOrNull(form.presupuestoMax),
    activa: true,

    // parámetros
    palabrasClave: trim(form.palabrasClave),
    descripcionActividad: trim(form.descripcionActividad),
    tiposContratos: trim(form.tiposContratos),
    contratosMenores: !!form.contratosMenores,
    lugares: trim(form.lugares) || "España",
    organosContratacion: trim(form.organosContratacion) || "Todos",

    // envío
    frecuenciaEnvio: envio.frecuencia as any,
    horaEnvio: trim(envio.hora) || "10:00",
    diaSemana: trim(envio.diaSemana) || "Lunes",
    notificarCambios: !!envio.notificarCambios,

    email1: trim(envio.email1),
    email2: trim(envio.email2),
    email3: trim(envio.email3),
  };
}

onMounted(async () => {
  if (!isNew && id != null) {
    try {
      const a: any = await (alertasApi as any).obtener?.(id) ?? await (alertasApi as any).getById?.(id);

      if (a) {
        // base
        envio.nombreAlerta = a.nombre ?? "";
        form.sectorCpv = a.cpvs ?? "";
        form.descripcionActividad = a.descripcionActividad ?? a.descripcion ?? "";
        form.palabrasClave = a.palabrasClave ?? "";
        form.tiposContratos = a.tiposContratos ?? "";
        form.contratosMenores = !!a.contratosMenores;
        form.lugares = a.lugares ?? "España";
        form.organosContratacion = a.organosContratacion ?? "Todos";

        form.presupuestoMin = a.importeMin == null ? "-" : String(a.importeMin);
        form.presupuestoMax = a.importeMax == null ? "-" : String(a.importeMax);

        // envío
        envio.frecuencia = a.frecuenciaEnvio ?? "DIARIAMENTE";
        envio.hora = a.horaEnvio ?? "10:00";
        envio.diaSemana = a.diaSemana ?? "Lunes";
        envio.notificarCambios = a.notificarCambios ?? true;
        envio.email1 = a.email1 ?? "";
        envio.email2 = a.email2 ?? "";
        envio.email3 = a.email3 ?? "";
      }
    } catch (e: any) {
      console.warn("No se pudo precargar alerta:", e);
      errorMsg.value = e?.message ?? "No se pudo precargar la alerta";
    }
  }
});

async function guardar() {
  errorMsg.value = "";
  saving.value = true;

  try {
    const payload = buildPayload();

    // Validaciones mínimas útiles
    if (!payload.nombre) {
      tab.value = "envio";
      errorMsg.value = "El nombre de la alerta es obligatorio.";
      return;
    }

    if (!payload.cpvs && !payload.descripcionActividad && !payload.palabrasClave) {
      tab.value = "params";
      errorMsg.value = "Rellena al menos CPVs, palabras clave o descripción de la actividad.";
      return;
    }

    if (!isValidEmail(payload.email1) || !isValidEmail(payload.email2) || !isValidEmail(payload.email3)) {
      tab.value = "envio";
      errorMsg.value = "Alguno de los emails no tiene formato válido.";
      return;
    }

    // Si no es semanal, el día no importa (pero mandamos uno por defecto para no romper)
    if (payload.frecuenciaEnvio !== "SEMANALMENTE") {
      payload.diaSemana = payload.diaSemana || "Lunes";
    }

    if (isNew) {
      await alertasApi.crear(payload);
    } else if (id != null) {
      await alertasApi.actualizar(id, payload);
    }

    router.push("/mis-alertas");
  } catch (e: any) {
    console.error(e);
    errorMsg.value = e?.message ?? "Error guardando la alerta";
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.page{
  background:#f1f3f7;
  min-height:calc(100vh - 56px);
  padding:16px 18px 26px;
  font-family:Inter,system-ui,Arial,sans-serif;
}

.kicker{
  font-size:11px;
  letter-spacing:.6px;
  color:#9aa4b2;
  margin:4px 0 10px 6px;
}

.content{
  width:100%;
  margin:0;
  padding:0;
}

.title{
  margin:0 0 14px;
  font-size:16px;
  font-weight:900;
  color:#1f2a44;
}

.panel{
  width:100%;
  background:#fff;
  border-radius:16px;
  border:1px solid #e8eef6;
  box-shadow:0 18px 44px rgba(16,24,40,.08);
  padding:14px 18px 22px;
}

.tabs{
  display:flex;
  gap:16px;
  padding:2px 6px 10px;
  border-bottom:1px solid #eef2f7;
  margin-bottom:18px;
}

.tab{
  display:inline-flex;
  align-items:center;
  gap:8px;
  background:transparent;
  border:0;
  padding:10px 10px;
  font-size:12px;
  font-weight:900;
  color:#7b869a;
  cursor:pointer;
  border-bottom:2px solid transparent;
}
.tab.active{
  color:#0b5ed7;
  border-bottom-color:#0b5ed7;
}
.ico{ font-size:13px; line-height:1; }

.form{ padding:6px 6px 0; }

.grid{
  display:grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap:22px 36px;
  align-items:start;
}

.col{
  display:flex;
  flex-direction:column;
  gap:16px;
  min-width:0;
}

.field label{
  display:block;
  font-size:12px;
  color:#6b7280;
  font-weight:700;
  margin-bottom:8px;
}

.field input{
  width:100%;
  height:42px;
  border:1px solid #d8e1ef;
  border-radius:10px;
  padding:0 12px;
  font-size:13px;
  background:#fff;
  color:#111827;
  box-sizing:border-box;
}

.field input:focus{
  outline:none;
  border-color:#0b5ed7;
  box-shadow:0 0 0 3px rgba(11,94,215,.12);
}

.row2{
  display:grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap:16px;
}

.toggle-row{
  display:flex;
  align-items:center;
  justify-content:flex-start;
  gap:18px;
  padding-top:2px;
}
.toggle-title{
  font-size:13px;
  font-weight:800;
  color:#374151;
}

.switch{ position:relative; width:46px; height:26px; display:inline-block; }
.switch input{ display:none; }

.slider{
  position:absolute;
  inset:0;
  border-radius:999px;
  background:#f3c0d2;
  border:1px solid #e9a9c2;
  transition:.2s;
}
.slider:before{
  content:'';
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
.switch input:checked + .slider{ background:#d06a8e; border-color:#d06a8e; }
.switch input:checked + .slider:before{ transform:translateX(20px); }

.btn-save{
  width:fit-content;
  height:38px;
  padding:0 14px;
  background:#0b5ed7;
  color:#fff;
  border:0;
  border-radius:10px;
  font-size:12px;
  font-weight:900;
  cursor:pointer;
  box-shadow:0 10px 22px rgba(11,94,215,.18);
}
.btn-save:disabled{
  opacity:.65;
  cursor:not-allowed;
}

@media (max-width: 1100px){
  .grid{
    grid-template-columns: 1fr;
    gap:18px;
  }
  .row2{
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px){
  .page{ padding:12px 12px 18px; }
  .panel{ padding:12px 12px 16px; border-radius:14px; }
  .title{ font-size:15px; }
}

.envio-wrap{
  width:100%;
  max-width: 920px;
}

.envio input,
.envio select{
  width:100%;
  height:42px;
  border:1px solid #d8e1ef;
  border-radius:10px;
  padding:0 12px;
  font-size:13px;
  background:#fff;
  color:#111827;
  box-sizing:border-box;
}

.envio input:focus,
.envio select:focus{
  outline:none;
  border-color:#0b5ed7;
  box-shadow:0 0 0 3px rgba(11,94,215,.12);
}

.envio .field label{
  display:block;
  font-size:12px;
  color:#6b7280;
  font-weight:700;
  margin-bottom:8px;
}

.row3{
  display:grid;
  grid-template-columns: 1fr 220px 260px;
  gap:16px;
  align-items:end;
  margin-top: 8px;
}

.toggle-line{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:16px;
  margin: 14px 0 10px;
}

.toggle-text{
  font-size:13px;
  font-weight:800;
  color:#374151;
  line-height:1.2;
  max-width: 620px;
}

.switch-blue{
  position: relative;
  width:46px;
  height:26px;
  display:inline-block;
  flex:0 0 auto;
}
.switch-blue input{ display:none; }

.slider-blue{
  position:absolute;
  inset:0;
  border-radius:999px;
  background: rgba(37, 99, 235, 0.25);
  border: 1px solid rgba(37, 99, 235, 0.35);
  transition:.2s;
}
.slider-blue:before{
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

.switch-blue input:checked + .slider-blue{
  background:#2563eb;
  border-color:#2563eb;
}
.switch-blue input:checked + .slider-blue:before{
  transform: translateX(20px);
}

.emails{
  margin-top: 10px;
  max-width: 520px;
}

.emails-label{
  display:block;
  font-size:12px;
  color:#6b7280;
  font-weight:700;
  margin-bottom:8px;
}

.emails input{
  margin-bottom: 12px;
}

@media (max-width: 900px){
  .row3{
    grid-template-columns: 1fr;
  }
  .toggle-text{
    max-width: 100%;
  }
  .emails{
    max-width: 100%;
  }
}

.error-box{
  margin: 10px 6px 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(185, 28, 28, 0.08);
  border: 1px solid rgba(185, 28, 28, 0.20);
  color: #b91c1c;
  font-weight: 800;
  font-size: 12px;
}
</style>
