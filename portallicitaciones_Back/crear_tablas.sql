USE portallicitaciones;
GO

-- 1. Tabla Usuarios
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Usuarios')
CREATE TABLE Usuarios (
    IdUsuario INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(100) NOT NULL UNIQUE,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    PasswordHash NVARCHAR(255) NOT NULL,
    Rol NVARCHAR(50) NOT NULL CHECK (Rol IN ('USER', 'ADMIN')),
    Activo BIT NOT NULL DEFAULT 1,
    FechaCreacion DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- 2. Tabla Licitaciones
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Licitaciones')
CREATE TABLE Licitaciones (
    IdLicitacion INT IDENTITY(1,1) PRIMARY KEY,
    Expediente NVARCHAR(100) NOT NULL,
    Titulo NVARCHAR(500) NOT NULL,
    Organismo NVARCHAR(500) NOT NULL,
    Presupuesto DECIMAL(18,2) NULL,
    FechaPublicacion DATE NULL,
    FechaLimite DATE NULL,
    Url NVARCHAR(1000) NULL,
    Estado NVARCHAR(50) NOT NULL DEFAULT 'ABIERTA',
    FechaCreacion DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

-- 3. Tabla CPV
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='CPV')
CREATE TABLE CPV (
    IdCPV INT IDENTITY(1,1) PRIMARY KEY,
    Codigo NVARCHAR(20) NOT NULL UNIQUE,
    Descripcion NVARCHAR(500) NOT NULL
);

-- 4. Relación Licitacion-CPV
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='LicitacionCPV')
CREATE TABLE LicitacionCPV (
    IdLicitacion INT NOT NULL,
    IdCPV INT NOT NULL,
    CONSTRAINT PK_LicitacionCPV PRIMARY KEY (IdLicitacion, IdCPV),
    CONSTRAINT FK_LicitacionCPV_Licitacion FOREIGN KEY (IdLicitacion)
        REFERENCES Licitaciones(IdLicitacion) ON DELETE CASCADE,
    CONSTRAINT FK_LicitacionCPV_CPV FOREIGN KEY (IdCPV)
        REFERENCES CPV(IdCPV) ON DELETE CASCADE
);

-- 5. Usuario guarda licitación
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='UsuarioLicitacion')
CREATE TABLE UsuarioLicitacion (
    IdUsuario INT NOT NULL,
    IdLicitacion INT NOT NULL,
    Estado NVARCHAR(50) NOT NULL DEFAULT 'GUARDADA',
    FechaAccion DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT PK_UsuarioLicitacion PRIMARY KEY (IdUsuario, IdLicitacion),
    CONSTRAINT FK_UL_Usuario FOREIGN KEY (IdUsuario)
        REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE,
    CONSTRAINT FK_UL_Licitacion FOREIGN KEY (IdLicitacion)
        REFERENCES Licitaciones(IdLicitacion) ON DELETE CASCADE
);

-- 6. Histórico
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='LicitacionHistorico')
CREATE TABLE LicitacionHistorico (
    IdHistorico INT IDENTITY(1,1) PRIMARY KEY,
    IdLicitacion INT NOT NULL,
    Campo NVARCHAR(100) NOT NULL,
    ValorAnterior NVARCHAR(MAX) NULL,
    ValorNuevo NVARCHAR(MAX) NULL,
    FechaCambio DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Historico_Licitacion FOREIGN KEY (IdLicitacion)
        REFERENCES Licitaciones(IdLicitacion) ON DELETE CASCADE
);

-- 7. Analítica
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='LicitacionAnalitica')
CREATE TABLE LicitacionAnalitica (
    IdAnalitica INT IDENTITY(1,1) PRIMARY KEY,
    IdLicitacion INT NOT NULL,
    Fuente NVARCHAR(100) NOT NULL,
    NumeroLotes INT NULL,
    Procedimiento NVARCHAR(100) NULL,
    TipoContrato NVARCHAR(100) NULL,
    FechaAnalisis DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Analitica_Licitacion FOREIGN KEY (IdLicitacion)
        REFERENCES Licitaciones(IdLicitacion) ON DELETE CASCADE
);

-- 8. Índices
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Licitaciones_FechaLimite')
CREATE INDEX IX_Licitaciones_FechaLimite ON Licitaciones(FechaLimite);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_Licitaciones_Presupuesto')
CREATE INDEX IX_Licitaciones_Presupuesto ON Licitaciones(Presupuesto);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_CPV_Codigo')
CREATE INDEX IX_CPV_Codigo ON CPV(Codigo);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='IX_UsuarioLicitacion_Estado')
CREATE INDEX IX_UsuarioLicitacion_Estado ON UsuarioLicitacion(Estado);
GO
