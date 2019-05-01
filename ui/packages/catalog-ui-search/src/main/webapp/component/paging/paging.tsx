import * as React from 'react'
import styled from '../../react-component/styles/styled-components'
import { hot } from 'react-hot-loader'
import {
  Button,
  buttonTypeEnum,
} from '../../react-component/presentation/button'

type Props = {
  page: number
  hasNextServerPage: boolean
  hasPreviousServerPage: boolean
  onClickNext: () => void
  onClickPrevious: () => void
}

const Root = styled.div`
  display: flex;
  justify-content: space-between;
  padding: ${props => props.theme.minimumSpacing};
`

const CustomButton = styled(Button)`
  padding: 0 ${props => props.theme.minimumSpacing};
`

const PageLabel = styled.div`
  padding: 0 ${props => props.theme.minimumSpacing};
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  font-size: ${props => props.theme.largeFontSize};
`

const Paging = (props: Props) => {
  const {
    page,
    hasNextServerPage,
    hasPreviousServerPage,
    onClickNext,
    onClickPrevious,
  } = props
  return (
    <Root>
      <CustomButton
        buttonType={buttonTypeEnum.primary}
        onClick={onClickPrevious}
        disabled={!hasPreviousServerPage}
      >
        <span>Previous</span>
      </CustomButton>
      <PageLabel>
        <span>{`Page ${page}`}</span>
      </PageLabel>
      <CustomButton
        disabled={!hasNextServerPage}
        buttonType={buttonTypeEnum.primary}
        onClick={onClickNext}
      >
        <span>Next</span>
      </CustomButton>
    </Root>
  )
}

export default hot(module)(Paging)
