package com.ordenatec.portallicitaciones.api.controller;

import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioEntity;
import com.ordenatec.portallicitaciones.infra.persistence.entity.UsuarioLicitacionEntity;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioJpaRepository;
import com.ordenatec.portallicitaciones.infra.persistence.repository.UsuarioLicitacionJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/ingestion")
public class IngestionController {

    private final UsuarioJpaRepository usuarioRepo;
    private final UsuarioLicitacionJpaRepository usuarioLicitRepo;

    @Value("${app.ingest.token:}")
    private String ingestToken;

    public IngestionController(UsuarioJpaRepository usuarioRepo,
                               UsuarioLicitacionJpaRepository usuarioLicitRepo) {
        this.usuarioRepo = usuarioRepo;
        this.usuarioLicitRepo = usuarioLicitRepo;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> ingest(
            @RequestHeader(value = "X-INGEST-TOKEN", required = false) String token,
            @RequestBody IngestBody body
    ) {
        // 0) Token
        if (ingestToken == null || ingestToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", "Ingest token no configurado (app.ingest.token)"));
        }
        if (token == null || !token.equals(ingestToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "error", "Token inválido"));
        }

        // 1) Body
        if (body == null || body.cliente == null || isBlank(body.cliente.email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "Falta cliente.email"));
        }

        String emailCliente = body.cliente.email.trim();

        // 2) Buscar usuario (email_destino -> email)
        Optional<UsuarioEntity> optUsuario = usuarioRepo.findByEmailDestinoIgnoreCase(emailCliente);
        if (optUsuario.isEmpty()) optUsuario = usuarioRepo.findByEmailIgnoreCase(emailCliente);

        if (optUsuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ok", false, "error", "No existe usuario para ese email", "email", emailCliente));
        }

        UsuarioEntity usuario = optUsuario.get();

        // 3) Insertar (sin duplicar por UNIQUE)
        LocalDate fechaEnvio = LocalDate.now();

        SaveResult r1 = saveItems(usuario, "LICIT", body.licitaciones, fechaEnvio);
        SaveResult r2 = saveItems(usuario, "MENOR", body.contratos_menores, fechaEnvio);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "id_usuario", usuario.getIdUsuario(),
                "email_in", emailCliente,
                "inserted", r1.inserted + r2.inserted,
                "duplicates_ignored", r1.duplicates + r2.duplicates
        ));
    }

    // ---------------- Guardado ----------------

    private SaveResult saveItems(UsuarioEntity usuario, String tipo, List<Item> items, LocalDate fechaEnvio) {
        if (items == null || items.isEmpty()) return new SaveResult(0, 0);

        int ins = 0;
        int dups = 0;

        for (Item it : items) {
            String idExterno = firstNonBlank(it.id_externo, it.expediente, it.uuid);
            String enlace = firstNonBlank(it.enlace, it.urlPublica, it.url);

            // Si no hay enlace ni id, al menos guarda algo: (pero intentamos evitar basura)
            if (isBlank(idExterno) && isBlank(enlace) && isBlank(it.titulo)) {
                continue;
            }

            UsuarioLicitacionEntity row = new UsuarioLicitacionEntity();
            row.setUsuario(usuario);
            row.setTipo(tipo);
            row.setIdExterno(trimToNull(idExterno));
            row.setTitulo(trimToNull(it.titulo));
            row.setOrganismo(trimToNull(firstNonBlank(it.organismo, it.organo, it.entidad)));
            row.setEnlace(trimToNull(enlace));
            row.setCpv(trimToNull(it.cpv));
            row.setFuente(trimToNull(firstNonBlank(it.fuente, it.origen)));
            row.setFechaEnvio(fechaEnvio);
            row.setFechaCreacion(LocalDateTime.now());

            // importe (admite precioLicitacion, valorEstimado, importe)
            row.setImporte(parseBigDecimal(firstNonNull(it.importe, it.precioLicitacion, it.valorEstimado)));

            // fecha límite (admite fecha_limite, fechaLimitePresentacion, etc.)
            row.setFechaLimite(parseDateFlexible(firstNonBlank(
                    it.fecha_limite,
                    it.fechaLimitePresentacion,
                    it.fechaLimite,
                    it.fecha_fin
            )));

            try {
                usuarioLicitRepo.save(row);
                ins++;
            } catch (DataIntegrityViolationException dup) {
                dups++;
            }
        }

        return new SaveResult(ins, dups);
    }

    private record SaveResult(int inserted, int duplicates) { }

    // ---------------- Utils ----------------

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Object firstNonNull(Object... vals) {
        if (vals == null) return null;
        for (Object v : vals) {
            if (v != null) return v;
        }
        return null;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static BigDecimal parseBigDecimal(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());

            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;

            s = s.replace("€", "").replace(" ", "");
            if (s.contains(",") && s.contains(".")) s = s.replace(".", "").replace(",", ".");
            else s = s.replace(",", ".");
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate parseDateFlexible(String v) {
        if (isBlank(v)) return null;
        String s = v.trim();
        try {
            // dd/MM/yyyy
            if (s.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            // yyyy-MM-dd (o con hora)
            if (s.matches("^\\d{4}-\\d{2}-\\d{2}.*$")) {
                return LocalDate.parse(s.substring(0, 10));
            }
            // dd-MM-yyyy
            if (s.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- DTO Request ----------------
    public static class IngestBody {
        public Cliente cliente;
        public List<Item> licitaciones;
        public List<Item> contratos_menores;
    }

    public static class Cliente {
        public String nif;
        public String nombre;
        public String email;
    }

    /**
     * DTO tolerante: soporta nombres que sueles usar en tus JSON
     */
    public static class Item {
        // “identificadores”
        public String id_externo;  // tu campo “normalizado”
        public String expediente;  // frecuente
        public String uuid;        // frecuente

        // “enlaces”
        public String enlace;      // normalizado
        public String urlPublica;  // frecuente
        public String url;         // alternativo

        // “texto”
        public String titulo;
        public String organismo;   // normalizado
        public String organo;      // frecuente
        public String entidad;     // alternativo

        // “fechas”
        public String fecha_limite;            // normalizado
        public String fechaLimitePresentacion; // frecuente
        public String fechaLimite;             // alternativo
        public String fecha_fin;               // alternativo (por si algún origen lo llama así)

        // “importe”
        public Object importe;          // normalizado
        public Object precioLicitacion; // frecuente
        public Object valorEstimado;    // frecuente

        // meta
        public String cpv;
        public String fuente; // normalizado
        public String origen; // alternativo
    }
}
