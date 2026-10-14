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

  # Productos del cliente: 38705 (ahorro), 38706 (corriente), 38707 (prestamos / libre inversion), 38708 (rotativo),
  # 38702 (pre-aprobado) y 38709 (saldos). El producto viene del caso o de la columna TipoProducto del control y el
  # monto esperado de la columna Monto. Con AUTO se busca en el listado un cliente que muestre el producto.
  # Los textos validados son los observados en la app (control de cambios en Consolidacion_Casos_de_Prueba.docx).
  # En 38709 solo se validan el formato y el valor del saldo: la informacion de cada tarjeta es de 38705 a 38708.
  @productos @38705 @38706 @38707 @38708 @38702 @38709 @HappyPath
  Scenario: Producto del cliente en el Home
    Given que un cliente registrado con el producto del archivo de control inició sesión en la app
    Then ve el producto en su sección del Home con su información y formato
    And el monto del producto coincide con el esperado del archivo de control

  # El skeleton dura pocos segundos (3 a 4 observados): se observa dentro del paso de ingreso, antes de las capturas
  # automaticas de cada paso. Durante la carga, Tus cuentas y Tus creditos muestran el texto "Cargando...".
  @38698
  Scenario: Skeleton de carga mientras el Home obtiene los saldos
    Given que un cliente registrado inicia sesión y el Home empieza a cargar
    Then mientras cargan los saldos ve el indicador de carga en las secciones de productos
    And al terminar la carga ve sus productos con los montos
