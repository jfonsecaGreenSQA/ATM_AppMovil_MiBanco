@home
Feature: Home de la App Mi Banco
  Como cliente registrado de Mibanco
  Quiero ver mi información y mis accesos al ingresar a la app
  Para usar los servicios digitales desde el Home

  # El cliente y su clave vienen del archivo de control (AUTO = cliente REGISTRADO del listado).
  # Cada escenario cierra sesion al terminar (hooks).

  @38699 @HappyPath
  Scenario: Encabezado del Home con saludo y pregunta de actividad
    Given que un cliente registrado inició sesión en la app
    Then ve el saludo de bienvenida
    And ve la pregunta "¿Qué quieres hacer hoy?"

  @38700 @HappyPath
  Scenario: Encabezado del Home con último ingreso
    Given que un cliente registrado con ingresos previos inició sesión en la app
    Then ve el saludo de bienvenida
    And ve la pregunta "¿Qué quieres hacer hoy?"
    And ve la fecha y hora de su último ingreso

  # La identidad visual (fuentes, colores, logotipo) frente al documento de diseno se verifica manualmente.
  @HappyPath
  Scenario Outline: Acceso directo en la parte superior del Home
    Given que un cliente registrado inició sesión en la app
    Then ve el acceso "<acceso>" en la parte superior del Home

    @38703
    Examples: Bre-B
      | acceso |
      | Bre-B  |

    @38704
    Examples: Pedir Crédito
      | acceso        |
      | Pedir Crédito |

  # El estado activo de "Inicio" no se expone a la automatizacion: se verifica manualmente en la captura.
  @38711 @HappyPath
  Scenario: Menú inferior del Home
    Given que un cliente registrado inició sesión en la app
    Then ve el menú inferior con los accesos "Inicio, Operaciones, QR, Para ti, Más" en ese orden
