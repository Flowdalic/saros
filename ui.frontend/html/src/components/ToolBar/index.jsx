import { Button, Navbar } from 'react-bootstrap'
import { Text } from 'react-localize'

import ConnectionSplitButton from './ConnectionSplitButton'
import React from 'react'

const ToolBar = ({core, mainUI}) => (
  <Navbar fixed={"top"}>
    <ConnectionSplitButton
      accounts={core.accounts}
      onConnect={core.doConnect}
      onDisconnect={core.doDisconnect}
      connectionState={core.state.connectionState}
      />

    <Button id='add-contact' onClick={core.doShowAddContactPage}>
      <Text message='action.addContact' />
    </Button>

    <Button id='start-session' onClick={core.doShowShareProjectPage}>
      <Text message='action.startSession' />
    </Button>
  </Navbar>
)

export default ToolBar
