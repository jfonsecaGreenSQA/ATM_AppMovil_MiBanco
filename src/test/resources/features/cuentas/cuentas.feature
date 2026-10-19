@cuentas
Feature: Cuentas del cliente en la App Mi Banco
  Como cliente registrado de Mibanco
  Quiero consultar el detalle de mis cuentas
  Para conocer sus saldos y movimientos

  # El cliente viene del archivo de control (AUTO = cliente REGISTRADO del listado que muestre la cuenta de ahorros).
  # Cada escenario vuelve al Home y cierra sesion al terminar (hooks).

  # Los movimientos se aceptan como lista o como el aviso de que no hay movimientos en los ultimos 30 dias: ningun
  # cliente de prueba tiene movimientos (15/10/2026). La lista se valida en 39567 a 39571 cuando haya datos.
  @39566 @HappyPath
  Scenario: Detalle de la cuenta de ahorros
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    Then ve el tipo de cuenta y su número completo
    And ve la opción para compartir los datos de la cuenta
    And ve el saldo disponible y el saldo en canje con formato de moneda
    And ve las acciones rápidas de la cuenta
    And ve la sección de movimientos

  # Se abre el movimiento mas reciente. La app no muestra cuenta de origen ni de destino y el comprobante puede venir
  # en ceros (observado el 19/10/2026): quedan como observacion en el reporte (control de cambios).
  @39571 @HappyPath
  Scenario: Detalle de una transacción de la cuenta de ahorros
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    And abre el detalle de uno de sus movimientos
    Then ve el monto, la fecha y la hora del movimiento
    And ve el número de comprobante y la descripción del movimiento

  # La imagen descargada se adjunta a la evidencia y se borra del dispositivo. La legibilidad la revisa una persona.
  @39572 @HappyPath
  Scenario: Descargar el comprobante de una transacción
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    And abre el detalle de uno de sus movimientos
    And descarga el comprobante
    Then ve el aviso de comprobante descargado
    And la imagen del comprobante queda guardada en el dispositivo

  # El menu se cierra sin elegir app: no se envia nada. Muestra contactos del dispositivo, por eso el video se detiene
  # antes de abrirlo y del menu solo queda como evidencia su encabezado y su vista previa.
  @39573 @HappyPath
  Scenario: Compartir el comprobante de una transacción
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    And abre el detalle de uno de sus movimientos
    And comparte el comprobante
    Then se abre el menú de compartir del dispositivo con la imagen del comprobante

  # 39567 (principal), 39569 y 39570 (sin "Ver mas" ni paginacion): mismo recorrido de la lista. 39569 dice "listado
  # del Home", pero el Home no muestra movimientos: se valida en el detalle de cuenta (pendiente de confirmar).
  # Con menos de 20 movimientos el corte en 20 queda como observacion.
  @39567 @39569 @39570 @HappyPath
  Scenario: Movimientos de la cuenta limitados a las últimas 20 transacciones y sin paginación
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    And recorre la lista de movimientos hasta el final
    Then ve el aviso de las últimas 20 transacciones
    And la lista tiene como máximo 20 movimientos ordenados del más reciente al más antiguo
    And al final de la lista no hay opción de ver más ni paginación y no se cargan más movimientos

  # Sin movimientos de mas de un mes, la exclusion de los anteriores queda como observacion.
  @39568 @HappyPath
  Scenario: Movimientos de la cuenta dentro del último mes
    Given que un cliente registrado con cuenta de ahorros inició sesión en la app
    When abre el detalle de su cuenta de ahorros desde "Tus cuentas"
    And recorre la lista de movimientos hasta el final
    Then todos los movimientos tienen fecha dentro del último mes
