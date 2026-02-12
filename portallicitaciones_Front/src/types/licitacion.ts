export type Money = {
  amount: number
  currency: string
}

export type FechasProcedimiento = {
  fechaPublicacion?: string // ISO date
  fechaLimite?: string // si lo añades luego
}

export type Licitacion = {
  id: string
  expediente: string
  titulo: string

  organismo?: { nombre?: string } | string | null
  organo?: string | null
  entidad?: string | null

  urlPublica?: string | null
  estado?: string | null
  tipoContrato?: string | null
  procedimiento?: string | null

  fechas?: FechasProcedimiento | null

  presupuestoBase?: Money | null
  valorEstimado?: Money | number | null
  valorEstimadoSinIva?: number | null
  moneda?: string | null

  lugarEjecucion?: string | null
  codigoNuts?: string | null

  // arrays existentes en tu JSON
  cpvs?: any[]
  lotes?: any[]
  documentos?: any[]
}
